package com.vhoda.luquita

import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter

class WelcomePagerAdapter(activity: AppCompatActivity) : FragmentStateAdapter(activity) {

    // Cambiar el número de pantallas a 4 en lugar de 5, ya que tienes 4 fragmentos
    override fun getItemCount(): Int {
        return 4 // Reducido a 4 pantallas
    }

    // Crear los fragmentos según la posición
    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> WelcomeFragment()      // Primer fragmento
            1 -> ExplanationFragment()  // Segundo fragmento
            2 -> PermissionsFragment()  // Tercer fragmento
            3 -> FinalFragment()        // Cuarto fragmento
            else -> WelcomeFragment()   // Default en caso de error
        }
    }
}
