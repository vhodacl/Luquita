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
import android.os.Build

class PermissionsFragment : Fragment(R.layout.fragment_permissions) {
    private var cameraPermissionGranted = false
    private var galleryPermissionGranted = false

    private val galleryPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        android.Manifest.permission.READ_MEDIA_IMAGES
    } else {
        android.Manifest.permission.READ_EXTERNAL_STORAGE
    }

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
            checkAndNavigate()
        } else {
            Toast.makeText(
                requireContext(),
                "Permiso de cámara denegado",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private val requestGalleryPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        galleryPermissionGranted = isGranted
        if (isGranted) {
            saveGalleryPermissionGranted()
            Toast.makeText(
                requireContext(),
                "Permiso de galería concedido",
                Toast.LENGTH_SHORT
            ).show()
            checkAndNavigate()
        } else {
            Toast.makeText(
                requireContext(),
                "Permiso de galería denegado",
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

    private fun checkGalleryPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            galleryPermission
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun savePermissionGranted() {
        requireActivity().getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
            .edit()
            .putBoolean("CAMERA_PERMISSION_GRANTED", true)
            .apply()
    }

    private fun saveGalleryPermissionGranted() {
        requireActivity().getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
            .edit()
            .putBoolean("GALLERY_PERMISSION_GRANTED", true)
            .apply()
    }

    private fun navigateToFinal() {
        getViewPager()?.let { viewPager ->
            viewPager.currentItem = viewPager.currentItem + 1
        }
    }

    private fun checkAndNavigate() {
        if (cameraPermissionGranted && galleryPermissionGranted) {
            navigateToFinal()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnCamera: Button = view.findViewById(R.id.btnCameraPermission)
        val btnGallery: Button = view.findViewById(R.id.btnGalleryPermission)
        val btnContinue: Button = view.findViewById(R.id.btnContinue)

        cameraPermissionGranted = checkCameraPermission()
        galleryPermissionGranted = checkGalleryPermission()

        if (cameraPermissionGranted && galleryPermissionGranted) {
            savePermissionGranted()
            saveGalleryPermissionGranted()
            view.post {
                navigateToFinal()
            }
            return
        }

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

        btnGallery.setOnClickListener {
            if (!galleryPermissionGranted) {
                requestGalleryPermission.launch(galleryPermission)
            } else {
                Toast.makeText(
                    requireContext(),
                    "Ya tienes permiso para usar la galería",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        btnContinue.setOnClickListener {
            if (cameraPermissionGranted && galleryPermissionGranted) {
                navigateToFinal()
            } else {
                Toast.makeText(
                    requireContext(),
                    "Debes conceder ambos permisos para continuar",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}