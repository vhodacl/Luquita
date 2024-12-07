package com.vhoda.luquita

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.vhoda.luquita.databinding.ActivityEasterEggBinding

class EasterEggActivity : AppCompatActivity() {
    private lateinit var binding: ActivityEasterEggBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEasterEggBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Configurar la imagen del gato
        binding.imageView.setImageResource(R.drawable.soggy) // Asegúrate de tener una imagen llamada cat_image en res/drawable
    }
} 