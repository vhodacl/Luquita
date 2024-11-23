package com.vhoda.luquita

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2

class WelcomeActivity : AppCompatActivity() {
    private lateinit var viewPager: ViewPager2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_welcome)

        viewPager = findViewById(R.id.viewPager)
        viewPager.adapter = WelcomePagerAdapter(this)
        viewPager.isUserInputEnabled = false
    }

    fun nextPage() {
        if (viewPager.currentItem < (viewPager.adapter?.itemCount ?: 0) - 1) {
            viewPager.currentItem = viewPager.currentItem + 1
        }
    }

    fun finishWelcomeProcess() {
        Log.d("WelcomeActivity", "Finishing welcome process")
        // Guardar que ya no es la primera vez
        getSharedPreferences("app_prefs", MODE_PRIVATE)
            .edit()
            .putBoolean("is_first_time", false)
            .apply()

        // Navegar a MainActivity
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }
}
