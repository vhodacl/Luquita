package com.vhoda.luquita

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.viewpager2.widget.ViewPager2

class PermissionsFragment : Fragment(R.layout.fragment_permissions) {
    private var cameraPermissionGranted = false

    private fun getViewPager() = (activity as? WelcomeActivity)?.findViewById<ViewPager2>(R.id.viewPager)

    private val requestCameraPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        cameraPermissionGranted = isGranted
        if (isGranted) {
            savePermissionGranted()
            Toast.makeText(
                requireContext(),
                "Permiso de cámara concedido",
                Toast.LENGTH_SHORT
            ).show()
            navigateToFinal()
        } else {
            Toast.makeText(
                requireContext(),
                "Permiso de cámara denegado",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun checkCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            android.Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun savePermissionGranted() {
        requireActivity().getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
            .edit()
            .putBoolean("CAMERA_PERMISSION_GRANTED", true)
            .apply()
    }

    private fun navigateToFinal() {
        getViewPager()?.let { viewPager ->
            viewPager.currentItem = viewPager.currentItem + 1
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Inicializar botones
        val btnCamera: Button = view.findViewById(R.id.btnCameraPermission)
        val btnContinue: Button = view.findViewById(R.id.btnContinue)

        // Verificar si ya tenemos el permiso
        cameraPermissionGranted = checkCameraPermission()

        // Si ya tiene permisos, navegar automáticamente
        if (cameraPermissionGranted) {
            savePermissionGranted()
            view.post {
                navigateToFinal()
            }
            return
        }

        // Configurar el botón de solicitud de permiso de cámara
        btnCamera.setOnClickListener {
            if (!cameraPermissionGranted) {
                requestCameraPermission.launch(android.Manifest.permission.CAMERA)
            } else {
                Toast.makeText(
                    requireContext(),
                    "Ya tienes permiso para usar la cámara",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        // Configurar el botón de continuar
        btnContinue.setOnClickListener {
            if (cameraPermissionGranted) {
                navigateToFinal()
            } else {
                Toast.makeText(
                    requireContext(),
                    "Debes conceder el permiso de cámara para continuar",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}