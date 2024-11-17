package com.vhoda.luquita

import android.content.ClipData  // Import para ClipData
import android.content.ClipboardManager  // Import para ClipboardManager
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast  // Import para Toast
import androidx.appcompat.app.AppCompatActivity
import com.vhoda.luquita.databinding.ActivityResultBinding


class ResultActivity : AppCompatActivity() {

    private lateinit var binding: ActivityResultBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResultBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Obtener el texto desde el Intent
        val detectedText = intent.getStringExtra(EXTRA_DETECTED_TEXT)
        binding.resultTextView.text = detectedText ?: "No se detectó texto"

        binding.copyButton.setOnClickListener {
            val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("OCR Text", binding.resultTextView.text)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, "Texto copiado al portapapeles", Toast.LENGTH_SHORT).show()
        }

        // Puedes agregar aquí la lógica para copiar al portapapeles, etc.
    }

    companion object {
        const val EXTRA_DETECTED_TEXT = "com.vhoda.luquita.EXTRA_DETECTED_TEXT"
    }

}
