package com.vhoda.luquita

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.card.MaterialCardView

class MainActivity : AppCompatActivity() {

    // Declara el MaterialCardView en lugar del ImageView
    private lateinit var cardSimple: MaterialCardView
    private val PICK_IMAGE = 101

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Vincula el MaterialCardView
        cardSimple = findViewById(R.id.cardSimple)

        // Configurar el Toolbar
        val toolbar: androidx.appcompat.widget.Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        // Acción para abrir la galería - usando el MaterialCardView
        findViewById<MaterialCardView>(R.id.gallery).setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(intent, PICK_IMAGE)
        }

        // Acción para abrir la cámara - usando MaterialCardView
        findViewById<MaterialCardView>(R.id.camera).setOnClickListener {
            Toast.makeText(this, "Función de cámara no implementada aún", Toast.LENGTH_SHORT).show()
        }
    }

    // Manejo del resultado de la galería
    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE && resultCode == RESULT_OK) {
            val selectedImage: Uri? = data?.data
            // Puedes agregar alguna acción cuando se seleccione una imagen, por ejemplo, cambiar el fondo de la tarjeta.
            // cardSimple.setCardBackgroundColor(Color.parseColor("#FF5733")) // Si quisieras cambiar el color de la tarjeta.
        }
    }
}
