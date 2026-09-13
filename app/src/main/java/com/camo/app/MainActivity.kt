package com.camo.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat

class MainActivity : AppCompatActivity() {
    private val REQUEST_PERMISSIONS = 100
    private lateinit var statusText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        statusText = findViewById(R.id.statusText)
        val btnStart: Button = findViewById(R.id.btnStart)
        val btnStop: Button = findViewById(R.id.btnStop)

        btnStart.setOnClickListener {
            if (checkPermissions()) {
                sendBroadcast(Intent("com.camo.app.START_STREAM"))
                statusText.text = "✅ Démarré !\nFlux: http://[IP]:8080"
            } else {
                requestPermissions()
            }
        }

        btnStop.setOnClickListener {
            sendBroadcast(Intent("com.camo.app.STOP_STREAM"))
            statusText.text = "⏹️ Arrêté"
        }

        if (!checkPermissions()) {
            requestPermissions()
        } else {
            statusText.text = "✅ Prêt !\nDéclenchement à distance actif"
        }
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
