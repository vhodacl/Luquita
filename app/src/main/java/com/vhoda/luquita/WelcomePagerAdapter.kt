package com.vhoda.luquita

import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter

class WelcomePagerAdapter(activity: AppCompatActivity) : FragmentStateAdapter(activity) {

    // Cambiar el número de pantallas a 5 en lugar de 4, ya que tienes 5 fragmentos
    override fun getItemCount(): Int {
        return 5 // Cuatro pantallas
    }

    // Crear los fragmentos según la posición
    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> WelcomeFragment()      // Primer fragmento
            1 -> ExplanationFragment()  // Segundo fragmento
            2 -> NoticeFragment()       // Tercer fragmento
            3 -> PermissionsFragment()  // Cuarto fragmento (permisos)
            4 -> FinalFragment()        // Quinto fragmento (final)
            else -> WelcomeFragment()   // Default en caso de error
        }
    }
}
