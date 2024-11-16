package com.vhoda.luquita

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2

class NoticeFragment : Fragment(R.layout.fragment_notice) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnNext: Button = view.findViewById(R.id.btnNext)
        btnNext.setOnClickListener {
            // Navegar al siguiente fragmento
            (activity as WelcomeActivity).findViewById<ViewPager2>(R.id.viewPager).currentItem = 3
        }
    }
}
