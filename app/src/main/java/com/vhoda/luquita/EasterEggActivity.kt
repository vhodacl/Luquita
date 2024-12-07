package com.vhoda.luquita

import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.vhoda.luquita.databinding.ActivityEasterEggBinding

class EasterEggActivity : AppCompatActivity() {
    private lateinit var binding: ActivityEasterEggBinding
    private var mediaPlayer: MediaPlayer? = null // Variable para el MediaPlayer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEasterEggBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Configurar las barras transparentes
        setupTransparentBars()

        binding.imageView.setImageResource(R.drawable.soggy)
        // Configurar el click listener para la imagen
        binding.imageView.setOnClickListener {
            playSound() // Llamar a la función para reproducir el sonido
        }
    }

    private fun setupTransparentBars() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.statusBarColor = resources.getColor(android.R.color.transparent, theme)
            window.navigationBarColor = resources.getColor(android.R.color.transparent, theme)

            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
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
