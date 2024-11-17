package com.vhoda.luquita

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2

class WelcomeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_welcome)

        // Configurar ViewPager2
        val viewPager: ViewPager2 = findViewById(R.id.viewPager)
        viewPager.adapter = WelcomePagerAdapter(this)

        // Deshabilitar el swipe entre páginas para tener control total sobre la navegación
        viewPager.isUserInputEnabled = false
    }
}