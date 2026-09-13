package com.camo.app

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import kotlin.random.Random

class MainActivity : AppCompatActivity() {
    private lateinit var codeText: TextView
    private lateinit var statusText: TextView
    private var roomCode = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        codeText = findViewById(R.id.codeText)
        statusText = findViewById(R.id.statusText)
        val btnStart: Button = findViewById(R.id.btnStart)
        val btnCopy: Button = findViewById(R.id.btnCopy)
        val btnWatch: Button = findViewById(R.id.btnWatch)

        roomCode = genererCode()
        codeText.text = "🔑 $roomCode"

        btnStart.setOnClickListener {
            if (verifierPermission()) {
                ouvrirCamera()
            } else {
                demanderPermission()
            }
        }

        btnCopy.setOnClickListener {
            copierCode()
        }

        btnWatch.setOnClickListener {
            ouvrirVisionneur()
        }
    }

    private fun genererCode(): String {
        val mots = listOf("bleu", "soleil", "lune", "vent", "mer", "neige", "feu", "roche", "nuage", "riviere")
        val mot1 = mots.random()
        val mot2 = mots.random()
        val chiffres = (10..99).random()
        return "$mot1-$mot2-$chiffres"
    }

    private fun ouvrirCamera() {
        val url = "https://vdo.ninja/?room=$roomCode&push&label=Camera"
        val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url))
        startActivity(intent)
        statusText.text = "✅ Caméra en ligne !\nPartage : $roomCode"
    }

    private fun ouvrirVisionneur() {
        val url = "https://vdo.ninja/?room=$roomCode&view"
        val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url))
        startActivity(intent)
    }

    private fun copierCode() {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Code", roomCode)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, "✅ Code copié !", Toast.LENGTH_SHORT).show()
    }

    private fun verifierPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    }

    private fun demanderPermission() {
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 100)
    }
}
