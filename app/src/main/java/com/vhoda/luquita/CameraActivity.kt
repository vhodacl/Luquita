package com.vhoda.luquita

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager.LayoutParams
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.vhoda.luquita.databinding.ActivityCameraBinding
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import android.content.Intent


class CameraActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCameraBinding
    private var camera: Camera? = null
    private var isFlashOn = false
    private lateinit var cameraExecutor: ExecutorService
    private var isProcessingImage = false
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private var lastProcessedText = ""
    private var lastProcessTime = 0L

    private val CAMERA_PERMISSION_REQUEST_CODE = 1001
    private val PROCESS_INTERVAL = 1000L // Intervalo mayor entre procesamiento de frames para evitar múltiples detecciones

    private var isTextDetected = false // Bandera para limitar a un solo resultado

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Configuración de pantalla completa
        window.setFlags(
            LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )

        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION)

        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT

        binding = ActivityCameraBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inicializar el executor para la cámara
        cameraExecutor = Executors.newSingleThreadExecutor()

        setupUI()
        checkCameraPermission()
    }

    private fun setupUI() {
        binding.apply {
            btnFlash.setOnClickListener { toggleFlash() }
            btnCancel.setOnClickListener { finish() }

            // Configurar el TextView para el texto reconocido
            recognizedTextView.apply {
                setBackgroundResource(R.drawable.text_background) // Asegúrate de crear este drawable
                setPadding(16, 8, 16, 8)
                visibility = View.INVISIBLE
            }
        }
    }

    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                CAMERA_PERMISSION_REQUEST_CODE
            )
        } else {
            startCamera()
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()
                cameraProvider.unbindAll()

                // Configurar la vista previa
                val preview = Preview.Builder()
                    .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                    .build()
                    .also {
                        it.setSurfaceProvider(binding.cameraPreview.surfaceProvider)
                    }

                // Configurar el selector de cámara
                val cameraSelector = CameraSelector.Builder()
                    .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                    .build()

                // Configurar el análisis de imagen
                val imageAnalyzer = ImageAnalysis.Builder()
                    .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also {
                        it.setAnalyzer(cameraExecutor) { imageProxy ->
                            processImageIfNeeded(imageProxy)
                        }
                    }

                // Vincular casos de uso
                camera = cameraProvider.bindToLifecycle(
                    this,
                    cameraSelector,
                    preview,
                    imageAnalyzer
                )

                // Observar el estado del flash
                camera?.cameraInfo?.torchState?.observe(this) { torchState ->
                    isFlashOn = torchState == TorchState.ON
                    updateFlashIcon()
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error al iniciar la cámara", e)
                Toast.makeText(this, "Error al iniciar la cámara", Toast.LENGTH_SHORT).show()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun processImageIfNeeded(imageProxy: ImageProxy) {
        val currentTime = System.currentTimeMillis()
        if (!isProcessingImage && currentTime - lastProcessTime >= PROCESS_INTERVAL) {
            processImageForOCR(imageProxy)
        } else {
            imageProxy.close()
        }
    }

    private fun processImageForOCR(imageProxy: ImageProxy) {
        isProcessingImage = true
        lastProcessTime = System.currentTimeMillis()

        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    val detectedText = visionText.text.trim()
                    if (detectedText.isNotEmpty() && detectedText != lastProcessedText) {
                        lastProcessedText = detectedText
                        if (!isTextDetected) { // Solo enviar el primer texto detectado
                            isTextDetected = true
                            // Aquí redirigimos a ResultActivity con el texto detectado
                            val intent = Intent(this@CameraActivity, ResultActivity::class.java)
                            intent.putExtra(ResultActivity.EXTRA_DETECTED_TEXT, detectedText)
                            startActivity(intent)
                            finish()  // Finaliza la actividad de la cámara para no seguir procesando
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error al procesar texto: ${e.message}")
                }
                .addOnCompleteListener {
                    imageProxy.close()
                    isProcessingImage = false
                }
        } else {
            imageProxy.close()
            isProcessingImage = false
        }
    }

    private fun toggleFlash() {
        camera?.cameraControl?.enableTorch(!isFlashOn)
    }

    private fun updateFlashIcon() {
        val flashIcon = if (isFlashOn) R.drawable.flashlight else R.drawable.flashlight_off
        binding.btnFlashIcon.setImageResource(flashIcon)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera()
            } else {
                Toast.makeText(
                    this,
                    "Se necesita permiso de cámara para continuar",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    companion object {
        private const val TAG = "CameraActivity"
    }
}
