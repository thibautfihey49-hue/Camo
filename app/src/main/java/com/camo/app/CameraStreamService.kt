package com.camo.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.media.ImageReader
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import java.net.ServerSocket
import java.net.Socket
import java.nio.charset.StandardCharsets
import java.util.concurrent.atomic.AtomicBoolean

class CameraStreamService : Service() {
    companion object {
        const val ACTION_START = "com.camo.app.START"
        const val ACTION_STOP = "com.camo.app.STOP"
        const val NOTIFICATION_ID = 1001
        const val CHANNEL_ID = "CamoService"
        private const val PORT = 8080
        private const val TAG = "CamoStealth"
    }

    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private var imageReader: ImageReader? = null
    private var serverSocket: ServerSocket? = null
    private val isRunning = AtomicBoolean(false)
    private val frames = mutableListOf<ByteArray>()
    private val framesLock = Any()

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: return START_STICKY
        when (action) {
            ACTION_START -> startStreaming()
            ACTION_STOP -> stopStreaming()
        }
        return START_STICKY
    }

    private fun startStreaming() {
        if (isRunning.get()) return
        isRunning.set(true)
        Log.d(TAG, "Demarrage streaming")
        startForeground(NOTIFICATION_ID, buildNotification())
        Thread { runHttpServer() }.start()
        openCamera()
    }

    private fun stopStreaming() {
        isRunning.set(false)
        Log.d(TAG, "Arret streaming")
        try { serverSocket?.close() } catch (e: Exception) {}
        try { captureSession?.stopRepeating() } catch (e: Exception) {}
        try { captureSession?.close() } catch (e: Exception) {}
        try { cameraDevice?.close() } catch (e: Exception) {}
        try { imageReader?.close() } catch (e: Exception) {}
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        stopSelf()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Camo Service", NotificationManager.IMPORTANCE_DEFAULT)
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Camo Streaming")
            .setContentText("Actif sur le port 8080")
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setOngoing(true)
            .build()
    }

    private fun openCamera() {
        val manager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            val cameraId = manager.cameraIdList[0]
            imageReader = ImageReader.newInstance(640, 480, ImageFormat.JPEG, 2)
            manager.openCamera(cameraId, object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    cameraDevice = camera
                    createCaptureSession()
                }
                override fun onDisconnected(camera: CameraDevice) { stopStreaming() }
                override fun onError(camera: CameraDevice, error: Int) { stopStreaming() }
            }, null)
        } catch (e: Exception) {
            Log.e(TAG, "Erreur camera", e)
        }
    }

    private fun createCaptureSession() {
        val reader = imageReader ?: return
        val surface = reader.surface
        reader.setOnImageAvailableListener({ reader ->
            val image = reader.acquireNextImage() ?: return@setOnImageAvailableListener
            val planes = image.planes
            val buffer = planes[0].buffer
            val data = ByteArray(buffer.remaining())
            buffer.get(data)
            synchronized(framesLock) {
                frames.clear()
                frames.add(data)
            }
            image.close()
        }, null)

        val camera = cameraDevice ?: return
        val requestBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
        requestBuilder.addTarget(surface)
        camera.createCaptureSession(listOf(surface), object : CameraCaptureSession.StateCallback() {
            override fun onConfigured(session: CameraCaptureSession) {
                captureSession = session
                session.setRepeatingRequest(requestBuilder.build(), null, null)
            }
            override fun onConfigureFailed(session: CameraCaptureSession) {}
        }, null)
    }

    private fun runHttpServer() {
        try {
            serverSocket = ServerSocket(PORT)
            Log.d(TAG, "Serveur sur le port $PORT")
            while (isRunning.get() && !serverSocket!!.isClosed) {
                val client = serverSocket!!.accept()
                Thread { handleClient(client) }.start()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erreur serveur", e)
        }
    }

    private fun handleClient(client: Socket) {
        val boundary = "frame_" + System.currentTimeMillis()
        try {
            val output = client.getOutputStream()
            output.write("HTTP/1.0 200 OK\r\n".toByteArray(StandardCharsets.UTF_8))
            output.write(("Content-Type: multipart/x-mixed-replace; boundary=$boundary\r\n\r\n").toByteArray(StandardCharsets.UTF_8))
            while (isRunning.get() && !client.isClosed) {
                val frame = synchronized(framesLock) { frames.lastOrNull() }
                if (frame != null) {
                    output.write("--$boundary\r\n".toByteArray(StandardCharsets.UTF_8))
                    output.write("Content-Type: image/jpeg\r\n".toByteArray(StandardCharsets.UTF_8))
                    output.write("Content-Length: ${frame.size}\r\n\r\n".toByteArray(StandardCharsets.UTF_8))
                    output.write(frame)
                    output.write("\r\n".toByteArray(StandardCharsets.UTF_8))
                }
                Thread.sleep(100)
            }
        } catch (e: Exception) {}
        finally { try { client.close() } catch (ignored: Exception) {} }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}
