package com.vhoda.luquita

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.card.MaterialCardView
import android.content.Context

class MainActivity : AppCompatActivity() {
    private val PICK_IMAGE = 101

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Verificar si completó el onboarding y tiene permisos
        val prefs = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        val hasPermissions = prefs.getBoolean("CAMERA_PERMISSION_GRANTED", false)
        val completedOnboarding = prefs.getBoolean("ONBOARDING_COMPLETED", false)

        if (!hasPermissions || !completedOnboarding) {
            // Si no tiene permisos o no completó el onboarding, mostrar WelcomeActivity
            startActivity(Intent(this, WelcomeActivity::class.java))
            finish()
            return
        }

        // Si ya tiene permisos, mostrar contenido principal
        setContentView(R.layout.activity_main)

        // Configurar el Toolbar
        val toolbar: androidx.appcompat.widget.Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        // Vincula los botones de la interfaz
        findViewById<MaterialCardView>(R.id.gallery).setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(intent, PICK_IMAGE)
        }

        findViewById<MaterialCardView>(R.id.camera).setOnClickListener {
            Toast.makeText(this, "Función de cámara no implementada aún", Toast.LENGTH_SHORT).show()
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE && resultCode == RESULT_OK) {
            val selectedImage: Uri? = data?.data
            Toast.makeText(this, "Imagen seleccionada: $selectedImage", Toast.LENGTH_SHORT).show()
        }
    }
}