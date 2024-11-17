package com.vhoda.luquita

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.core.TorchState
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.vhoda.luquita.databinding.ActivityCameraBinding

class CameraActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCameraBinding
    private var camera: Camera? = null
    private var isFlashOn = false

    private val CAMERA_PERMISSION_REQUEST_CODE = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCameraBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), CAMERA_PERMISSION_REQUEST_CODE)
        } else {
            startCamera()
        }

        // Configurar el botón de flash
        binding.btnFlash.setOnClickListener {
            toggleFlash()
        }

        // Configurar el botón de cerrar
        binding.btnCancel.setOnClickListener {
            finish()
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()

                // Limpiar bindings anteriores
                cameraProvider.unbindAll()

                val preview = Preview.Builder().build()
                preview.setSurfaceProvider(binding.cameraPreview.surfaceProvider)

                val cameraSelector = CameraSelector.Builder()
                    .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                    .build()

                camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview)

                // Configurar el observador del estado del flash
                camera?.cameraInfo?.torchState?.observe(this) { torchState ->
                    isFlashOn = torchState == TorchState.ON
                    updateFlashIcon()
                }

            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this, "Error al iniciar la cámara", Toast.LENGTH_SHORT).show()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun toggleFlash() {
        camera?.let { cam ->
            isFlashOn = !isFlashOn
            cam.cameraControl.enableTorch(isFlashOn)
            updateFlashIcon()
        }
    }

    private fun updateFlashIcon() {
        val flashIcon = if (isFlashOn) R.drawable.flashlight else R.drawable.flashlight_off
        binding.btnFlashIcon.setImageResource(flashIcon)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera()
            } else {
                Toast.makeText(this, "Se necesita permiso de cámara para continuar", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }
}