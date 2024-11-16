package com.vhoda.luquita

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment

class FinalFragment : Fragment(R.layout.fragment_final) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnStartApp: Button = view.findViewById(R.id.btnStartApp)

        btnStartApp.setOnClickListener {
            val intent = Intent(requireContext(), MainActivity::class.java)
            startActivity(intent)
            activity?.finish() // Opcional: Finaliza la actividad actual (FinalFragment)
        }
    }
}
