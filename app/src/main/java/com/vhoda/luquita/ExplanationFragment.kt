package com.vhoda.luquita

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.button.MaterialButton

class ExplanationFragment : Fragment(R.layout.fragment_explanation) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnNext: MaterialButton = view.findViewById(R.id.btnContinue)
        btnNext.setOnClickListener {
            // Navegar al siguiente fragmento (PermissionsFragment)
            (activity as WelcomeActivity).findViewById<ViewPager2>(R.id.viewPager).currentItem = 2
        }
    }
}
