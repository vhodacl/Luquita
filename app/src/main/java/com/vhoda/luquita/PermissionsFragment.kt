package com.vhoda.luquita

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import androidx.activity.result.contract.ActivityResultContracts

class PermissionsFragment : Fragment(R.layout.fragment_permissions) {

    private var cameraPermissionGranted = false

    // Registro para solicitar permiso de cámara
    private val requestCameraPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        cameraPermissionGranted = isGranted
        if (isGranted) {
            Toast.makeText(
                requireContext(),
                "Permiso de cámara concedido",
                Toast.LENGTH_SHORT
            ).show()
            // Si el permiso es concedido, navega al siguiente fragmento
            loadFinalFragment()  // Navegar al FinalFragment directamente desde PermissionsFragment
        } else {
            Toast.makeText(
                requireContext(),
                "Permiso de cámara denegado",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // Verificar si el permiso de cámara ya está concedido
    private fun checkExistingPermissions() {
        cameraPermissionGranted = ContextCompat.checkSelfPermission(
            requireContext(),
            android.Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnCamera: Button = view.findViewById(R.id.btnCameraPermission)
        val btnContinue: Button = view.findViewById(R.id.btnContinue)

        // Verifica el estado del permiso al iniciar el fragmento
        checkExistingPermissions()

        // Acción para solicitar el permiso de cámara
        btnCamera.setOnClickListener {
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    android.Manifest.permission.CAMERA
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // Si el permiso no está concedido, lo solicitamos
                requestCameraPermission.launch(android.Manifest.permission.CAMERA)
            } else {
                // Si ya tenemos el permiso
                Toast.makeText(
                    requireContext(),
                    "Ya tienes permiso para usar la cámara",
                    Toast.LENGTH_SHORT
                ).show()
                cameraPermissionGranted = true
            }
        }

        // Acción para continuar solo si el permiso de cámara fue concedido
        btnContinue.setOnClickListener {
            if (cameraPermissionGranted) {
                // Si el permiso fue concedido, navega al siguiente fragmento
                loadFinalFragment()
            } else {
                // Si el permiso no fue concedido, muestra un mensaje
                Toast.makeText(
                    requireContext(),
                    "Debes conceder el permiso de cámara para continuar",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    // Función para cargar el FinalFragment
    private fun loadFinalFragment() {
        val finalFragment = FinalFragment()


    }
}
