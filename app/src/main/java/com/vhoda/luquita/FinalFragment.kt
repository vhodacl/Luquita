package com.vhoda.luquita

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import android.content.Context

class FinalFragment : Fragment(R.layout.fragment_final) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnStartApp: Button = view.findViewById(R.id.btnStartApp)

        btnStartApp.setOnClickListener {
            // Guardar que completó el onboarding
            requireActivity().getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
                .edit()
                .putBoolean("ONBOARDING_COMPLETED", true)
                .apply()

            // Iniciar MainActivity
            val intent = Intent(requireContext(), MainActivity::class.java)
            startActivity(intent)
            activity?.finish()
        }
    }
}