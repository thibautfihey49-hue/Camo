package com.camo.app

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.Collections

class MainActivity : AppCompatActivity() {
    private val REQUEST_PERMISSIONS = 100
    private lateinit var statusText: TextView
    private lateinit var ipText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        statusText = findViewById(R.id.statusText)
        ipText = findViewById(R.id.ipText)
        
        // ✅ Affiche l'IP dès l'ouverture
        ipText.text = "📡 IP : ${getLocalIPAddress()}"
        
        val btnStart: Button = findViewById(R.id.btnStart)
        val btnStop: Button = findViewById(R.id.btnStop)

        btnStart.setOnClickListener {
            if (checkPermissions()) {
                sendBroadcast(Intent("com.camo.app.START_STREAM"))
                val ip = getLocalIPAddress()
                statusText.text = "✅ DÉMARRÉ !\n\n🌐 Ouvre dans un navigateur :\nhttp://$ip:8080"
            } else {
                requestPermissions()
            }
        }

        btnStop.setOnClickListener {
            sendBroadcast(Intent("com.camo.app.STOP_STREAM"))
            statusText.text = "⏹️ ARRÊTÉ"
        }

        if (!checkPermissions()) {
            requestPermissions()
        } else {
            val ip = getLocalIPAddress()
            statusText.text = "✅ PRÊT !\n\n🌐 Flux disponible sur :\nhttp://$ip:8080"
        }
    }

    // ✅ FONCTION : RÉCUPÈRE L'IP LOCALE
    private fun getLocalIPAddress(): String {
        try {
            val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
            for (netInterface in interfaces) {
                val addrs = Collections.list(netInterface.inetAddresses)
                for (addr in addrs) {
                    if (!addr.isLoopbackAddress && addr is Inet4Address) {
                        return addr.hostAddress
                    }
                }
            }
        } catch (e: Exception) {
            return "Inconnue"
        }
        return "127.0.0.1"
    }

    private fun checkPermissions(): Boolean {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) return false
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED) return false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.FOREGROUND_SERVICE) != PackageManager.PERMISSION_GRANTED) return false
        }
        return true
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(this, arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.INTERNET,
            Manifest.permission.FOREGROUND_SERVICE,
            Manifest.permission.RECEIVE_BOOT_COMPLETED
        ), REQUEST_PERMISSIONS)
    }
}
