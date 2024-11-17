package com.vhoda.luquita

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.card.MaterialCardView
import android.content.Context
import com.google.android.material.transition.platform.MaterialContainerTransformSharedElementCallback
import androidx.core.app.ActivityOptionsCompat
import androidx.core.view.ViewCompat
import com.google.android.material.transition.platform.MaterialContainerTransform

class MainActivity : AppCompatActivity() {
    private val PICK_IMAGE = 101

    override fun onCreate(savedInstanceState: Bundle?) {
        // Configurar la transición de entrada
        window.sharedElementEnterTransition = MaterialContainerTransform().apply {
            addTarget(android.R.id.content)
            duration = 300L // Duración de la animación en milisegundos
        }

        // Configurar la transición de retorno
        window.sharedElementReturnTransition = MaterialContainerTransform().apply {
            addTarget(android.R.id.content)
            duration = 250L
        }

        // Establecer el callback para elementos compartidos
        setEnterSharedElementCallback(MaterialContainerTransformSharedElementCallback())

        super.onCreate(savedInstanceState)

        // Verificar si completó el onboarding y tiene permisos
        val prefs = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        val hasPermissions = prefs.getBoolean("CAMERA_PERMISSION_GRANTED", false)
        val completedOnboarding = prefs.getBoolean("ONBOARDING_COMPLETED", false)

        if (!hasPermissions || !completedOnboarding) {
            startActivity(Intent(this, WelcomeActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_main)

        val toolbar: androidx.appcompat.widget.Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        val cameraButton = findViewById<MaterialCardView>(R.id.camera)
        // Asignar un transition name único al botón de la cámara
        ViewCompat.setTransitionName(cameraButton, "camera_button")

        findViewById<MaterialCardView>(R.id.gallery).setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(intent, PICK_IMAGE)
        }

        cameraButton.setOnClickListener {
            val intent = Intent(this, CameraActivity::class.java)
            val options = ActivityOptionsCompat.makeSceneTransitionAnimation(
                this,
                cameraButton,
                ViewCompat.getTransitionName(cameraButton)!!
            )
            startActivity(intent, options.toBundle())
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