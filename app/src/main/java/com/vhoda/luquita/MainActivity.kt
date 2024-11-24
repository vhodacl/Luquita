package com.vhoda.luquita

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.vhoda.luquita.databinding.ActivityMainBinding
import android.view.WindowInsetsController
import android.view.WindowManager

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val SPLASH_DELAY = 2000L // 3 segundos

    companion object {
        private const val TAG = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Hacer los iconos de la barra de estado negros
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Para Android 11 (API 30) y superior
            window.insetsController?.setSystemBarsAppearance(
                WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
                WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Para Android 6.0 (API 23) hasta Android 10
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        }

        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT

        // Delay y navegación
        Handler(Looper.getMainLooper()).postDelayed({
            val isFirstTime = isFirstTime()
            Log.d(TAG, "Is first time: $isFirstTime")

            if (isFirstTime) {
                startActivity(Intent(this, WelcomeActivity::class.java))
            } else {
                startActivity(Intent(this, Inicio::class.java))
            }
            finish()
        }, SPLASH_DELAY)
    }

    private fun isFirstTime(): Boolean {
        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        return prefs.getBoolean("is_first_time", true)
    }
}