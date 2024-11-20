package com.vhoda.luquita

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.vhoda.luquita.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val PICK_IMAGE = 101
    private val REQUIRED_PERMISSIONS = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
    } else {
        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }
    private val REQUEST_CODE = 123

    companion object {
        private const val TAG = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate called")
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupTransparentBars()
        checkPermissions()
        setupClickListeners()
    }

    private fun setupTransparentBars() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.statusBarColor = resources.getColor(android.R.color.transparent, theme)
            window.navigationBarColor = resources.getColor(android.R.color.transparent, theme)

            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        }
    }

    private fun setupClickListeners() {
        binding.gallery.setOnClickListener {
            Log.d(TAG, "Gallery button clicked")
            if (hasStoragePermission()) {
                openGallery()
            } else {
                Log.d(TAG, "Requesting permissions")
                checkPermissions()
            }
        }

        binding.camera.setOnClickListener {
            val intent = Intent(this, CameraActivity::class.java)
            startActivity(intent)
        }
    }

    private fun openGallery() {
        Log.d(TAG, "Opening gallery")
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "image/*"
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        }
        try {
            startActivityForResult(intent, PICK_IMAGE)
        } catch (e: Exception) {
            Log.e(TAG, "Error opening gallery: ${e.message}")
            Toast.makeText(this, "Error al abrir la galería", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkPermissions() {
        Log.d(TAG, "Checking permissions")
        if (!hasStoragePermission()) {
            Log.d(TAG, "Requesting permissions")
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE)
        } else {
            Log.d(TAG, "Permissions already granted")
        }
    }

    private fun hasStoragePermission(): Boolean {
        val hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_MEDIA_IMAGES
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
        Log.d(TAG, "Has storage permission: $hasPermission")
        return hasPermission
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        Log.d(TAG, "onRequestPermissionsResult: requestCode=$requestCode")
        if (requestCode == REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Permission granted")
                openGallery()
            } else {
                Log.d(TAG, "Permission denied")
                Toast.makeText(
                    this,
                    "Se necesitan permisos para acceder a la galería",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.d(TAG, "onActivityResult: requestCode=$requestCode, resultCode=$resultCode")

        if (requestCode == PICK_IMAGE) {
            if (resultCode == RESULT_OK) {
                val imageUri: Uri? = data?.data
                Log.d(TAG, "Selected image URI: $imageUri")

                if (imageUri != null) {
                    try {
                        // Verificar que la URI es válida
                        contentResolver.getType(imageUri)?.let { mimeType ->
                            Log.d(TAG, "Image mime type: $mimeType")

                            // Tomar permisos persistentes
                            val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION
                            contentResolver.takePersistableUriPermission(imageUri, takeFlags)

                            // Navegar a CheckInImageActivity
                            Log.d(TAG, "Navigating to CheckInImageActivity")
                            navigateToCheckInImage(imageUri)
                        } ?: run {
                            Log.e(TAG, "Invalid image URI - null mime type")
                            Toast.makeText(this, "Imagen no válida", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: SecurityException) {
                        Log.e(TAG, "Security error: ${e.message}")
                        // Intentar navegar sin permisos persistentes
                        navigateToCheckInImage(imageUri)
                    } catch (e: Exception) {
                        Log.e(TAG, "Unexpected error: ${e.message}")
                        Toast.makeText(this, "Error al procesar la imagen", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Log.e(TAG, "Null image URI")
                    Toast.makeText(this, "No se pudo obtener la imagen", Toast.LENGTH_SHORT).show()
                }
            } else {
                Log.d(TAG, "Image selection cancelled or failed")
                Toast.makeText(this, "Selección de imagen cancelada", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun navigateToCheckInImage(imageUri: Uri) {
        Log.d(TAG, "Creating intent for CheckInImageActivity")
        val intent = Intent(this, CheckInImageActivity::class.java).apply {
            putExtra("image_uri", imageUri.toString())
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try {
            Log.d(TAG, "Starting CheckInImageActivity")
            startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error starting CheckInImageActivity: ${e.message}")
            Toast.makeText(this, "Error al abrir el procesador de imágenes", Toast.LENGTH_LONG).show()
        }
    }
}