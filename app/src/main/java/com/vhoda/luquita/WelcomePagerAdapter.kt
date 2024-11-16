package com.vhoda.luquita

import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter

class WelcomePagerAdapter(activity: AppCompatActivity) : FragmentStateAdapter(activity) {

    override fun getItemCount(): Int {
        return 4 // Cuatro pantallas
    }

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> WelcomeFragment()
            1 -> ExplanationFragment()
            2 -> NoticeFragment()
            3 -> PermissionsFragment()
            else -> WelcomeFragment()
        }
    }
}
