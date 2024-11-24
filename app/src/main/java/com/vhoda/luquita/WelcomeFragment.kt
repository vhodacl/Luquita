package com.vhoda.luquita

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import android.view.WindowInsetsController
import android.graphics.Color

class WelcomeFragment : Fragment(R.layout.fragment_welcome) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupTransparentBars()

        val btnNext: Button = view.findViewById(R.id.btnNext)
        btnNext.setOnClickListener {
            (activity as? WelcomeActivity)?.nextPage()
        }
    }

    private fun setupTransparentBars() {
        requireActivity().window.apply {
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                    statusBarColor = resources.getColor(R.color.md_theme_background, requireActivity().theme)
                    navigationBarColor = resources.getColor(R.color.md_theme_background, requireActivity().theme)
                    
                    decorView.systemUiVisibility = (
                            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    )
                    
                    isStatusBarContrastEnforced = true
                    decorView.windowInsetsController?.setSystemBarsAppearance(
                        WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
                        WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                    )
                }
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                    statusBarColor = Color.TRANSPARENT
                    navigationBarColor = Color.TRANSPARENT
                    
                    var flags = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                               View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                               View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION

                    if (resources.getBoolean(R.bool.isLightTheme)) {
                        flags = flags or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            flags = flags or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
                        }
                    }
                    
                    decorView.systemUiVisibility = flags
                }
                else -> {
                    statusBarColor = Color.TRANSPARENT
                    decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                }
            }
        }
    }
}
