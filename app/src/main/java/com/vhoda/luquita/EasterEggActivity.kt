package com.vhoda.luquita

import android.media.MediaPlayer
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.vhoda.luquita.databinding.ActivityEasterEggBinding

class EasterEggActivity : AppCompatActivity() {
    private lateinit var binding: ActivityEasterEggBinding
    private var mediaPlayer: MediaPlayer? = null // Variable para el MediaPlayer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEasterEggBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.imageView.setImageResource(R.drawable.soggy)
        // Configurar el click listener para la imagen
        binding.imageView.setOnClickListener {
            playSound() // Llamar a la función para reproducir el sonido
        }
    }

    private fun playSound() {
        // Reproducir el sonido
        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer.create(this, R.raw.sog) // Cargar el archivo de sonido
        }
        mediaPlayer?.start() // Iniciar la reproducción
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release() // Liberar recursos del MediaPlayer
        mediaPlayer = null
    }
}