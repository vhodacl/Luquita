package com.vhoda.luquita

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2

class WelcomeFragment : Fragment(R.layout.fragment_welcome) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnNext: Button = view.findViewById(R.id.btnNext)
        btnNext.setOnClickListener {
            (activity as? WelcomeActivity)?.nextPage()
        }
    }
}
