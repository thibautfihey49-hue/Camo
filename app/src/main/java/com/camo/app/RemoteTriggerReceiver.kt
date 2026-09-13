package com.camo.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class RemoteTriggerReceiver : BroadcastReceiver() {
    companion object {
        const val TAG = "CamoTrigger"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        context ?: return
        val action = intent?.action ?: return

        when (action) {
            "com.camo.app.START_STREAM" -> {
                Log.d(TAG, "📡 START_STREAM reçu")
                val serviceIntent = Intent(context, CameraStreamService::class.java).apply {
                    action = CameraStreamService.ACTION_START
                }
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }
            }
            "com.camo.app.STOP_STREAM" -> {
                Log.d(TAG, "📡 STOP_STREAM reçu")
                val serviceIntent = Intent(context, CameraStreamService::class.java).apply {
                    action = CameraStreamService.ACTION_STOP
                }
                context.startService(serviceIntent)
            }
        }
    }
}
