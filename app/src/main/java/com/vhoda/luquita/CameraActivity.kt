package com.vhoda.luquita

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.*
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
import android.graphics.RectF
import android.media.Image
import androidx.camera.core.Camera
import androidx.camera.view.PreviewView
import java.io.ByteArrayOutputStream
import android.graphics.YuvImage
import android.graphics.ImageFormat
import kotlin.math.roundToInt

class CameraActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCameraBinding
    private var camera: Camera? = null
    private var isFlashOn = false
    private lateinit var cameraExecutor: ExecutorService
    private var isProcessingImage = false
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private var lastProcessedText = ""
    private var lastProcessTime = 0L
    private lateinit var scanRegion: RectF
    private var previewWidth: Int = 0
    private var previewHeight: Int = 0
    private val CAMERA_PERMISSION_REQUEST_CODE = 1001
    private val PROCESS_INTERVAL = 1000L
    private var isTextDetected = false

    data class BankData(
        var companyName: String? = null,
        var rut: String? = null,
        var bank: String? = null,
        var accountType: String? = null,
        var accountNumber: String? = null,
        var email: String? = null
    ) {
        fun isValid(): Boolean {
            var validFields = 0
            if (!companyName.isNullOrBlank()) validFields++
            if (!rut.isNullOrBlank() && rut!!.matches(Regex("\\d{1,2}\\d{3}\\d{3}-[\\dkK]"))) validFields++
            if (!bank.isNullOrBlank()) validFields++
            if (!accountType.isNullOrBlank()) validFields++
            if (!accountNumber.isNullOrBlank()) validFields++
            if (!email.isNullOrBlank() && email!!.contains("@")) validFields++
            return validFields >= 3
        }

        fun toMap(): Map<String, String> {
            return mapOf(
                "Nombre" to (companyName ?: ""),
                "RUT" to (rut ?: ""),
                "Banco" to (bank ?: ""),
                "Tipo de Cuenta" to (accountType ?: ""),
                "Número de Cuenta" to (accountNumber ?: ""),
                "Correo" to (email ?: "")
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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

        cameraExecutor = Executors.newSingleThreadExecutor()

        binding.cameraPreview.post {
            initializeScanRegion()
        }

        setupUI()
        checkCameraPermission()
    }

    private fun initializeScanRegion() {
        previewWidth = binding.cameraPreview.width
        previewHeight = binding.cameraPreview.height

        val scanFrame = binding.scanFrameContainer
        val scanFrameLocation = IntArray(2)
        scanFrame.getLocationInWindow(scanFrameLocation)

        scanRegion = RectF(
            scanFrameLocation[0].toFloat() / previewWidth,
            scanFrameLocation[1].toFloat() / previewHeight,
            (scanFrameLocation[0] + scanFrame.width).toFloat() / previewWidth,
            (scanFrameLocation[1] + scanFrame.height).toFloat() / previewHeight
        )
    }

    private fun setupUI() {
        binding.apply {
            btnFlash.setOnClickListener { toggleFlash() }
            btnCancel.setOnClickListener { finish() }
            btnResult.setOnClickListener {
                val detectedText = recognizedTextView.text.toString()
                if (detectedText.isNotEmpty()) {
                    val bankData = parseBankData(detectedText)
                    val intent = Intent(this@CameraActivity, ResultActivity::class.java).apply {
                        putExtra(ResultActivity.EXTRA_DETECTED_TEXT, detectedText)
                        putExtra("BANK_DATA", bankData.toMap().toString())
                    }
                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(this@CameraActivity, "No se ha detectado texto válido", Toast.LENGTH_SHORT).show()
                }
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

                val preview = Preview.Builder()
                    .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                    .build()
                    .also {
                        it.setSurfaceProvider(binding.cameraPreview.surfaceProvider)
                    }

                val cameraSelector = CameraSelector.Builder()
                    .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                    .build()

                val imageAnalyzer = ImageAnalysis.Builder()
                    .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also {
                        it.setAnalyzer(cameraExecutor) { imageProxy ->
                            processImageIfNeeded(imageProxy)
                        }
                    }

                camera = cameraProvider.bindToLifecycle(
                    this,
                    cameraSelector,
                    preview,
                    imageAnalyzer
                )

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

        try {
            val bitmap = imageProxy.toBitmap()
            if (bitmap != null) {
                val croppedBitmap = cropBitmap(bitmap, imageProxy.imageInfo.rotationDegrees)
                if (croppedBitmap != null) {
                    val image = InputImage.fromBitmap(croppedBitmap, 0)

                    recognizer.process(image)
                        .addOnSuccessListener { visionText ->
                            val detectedText = visionText.text.trim()
                            if (detectedText.isNotEmpty() && detectedText != lastProcessedText) {
                                lastProcessedText = detectedText

                                // Mostrar texto detectado y activar botón de resultado
                                binding.recognizedTextView.text = detectedText
                                binding.recognizedTextView.visibility = View.VISIBLE
                                binding.btnResult.visibility = View.VISIBLE
                            }
                        }
                        .addOnFailureListener { e ->
                            Log.e(TAG, "Error al procesar texto: ${e.message}")
                        }
                        .addOnCompleteListener {
                            croppedBitmap.recycle()
                            bitmap.recycle()
                            imageProxy.close()
                            isProcessingImage = false
                        }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error en processImageForOCR: ${e.message}")
            imageProxy.close()
            isProcessingImage = false
        }
    }


    private fun parseBankData(text: String): BankData {
        val bankData = BankData()
        val lines = text.split("\n")

        // Lista de palabras clave de bancos conocidos
        val knownBanks = mapOf(
            "estado" to "Banco Estado",
            "cuenta rut" to "Banco Estado",
            "cta rut" to "Banco Estado",
            "ctarut" to "Banco Estado",
            "cta.rut" to "Banco Estado",
            "bci" to "Banco Crédito e Inversiones",
            "banco chile" to "Banco de Chile",
            "banco de chile" to "Banco de Chile",
            "Santander" to "Banco Santander",
            "santander" to "Banco Santander",
            "mercado pago" to "Mercado Pago",
            "ripley" to "Banco Ripley",
            "falabella" to "Banco Falabella",
            "scotiabank" to "Scotiabank",
            "itau" to "Itaú",
            "tenpo" to "Tenpo",
            "tapp" to "TAPP",
            "copec" to "Copec",
            "mach" to "MACH"
        )

        for (line in lines) {
            val normalizedLine = line.trim().lowercase()

            when {
                // Detectar bancos basados en palabras clave conocidas
                knownBanks.keys.any { normalizedLine.contains(it) } -> {
                    val matchedBank = knownBanks.entries.find { normalizedLine.contains(it.key) }
                    if (matchedBank != null) {
                        bankData.bank = matchedBank.value
                    }
                }

                // Lógica específica para Banco Estado
                normalizedLine.contains("banco estado") ||
                        normalizedLine.contains("cuenta rut") ||
                        normalizedLine.contains("cta rut") -> {
                    bankData.bank = "Banco Estado"
                    bankData.accountType = when {
                        normalizedLine.contains("cuenta rut") || normalizedLine.contains("cta rut") -> "Cuenta RUT"
                        normalizedLine.contains("cuenta vista") -> "Cuenta Vista"
                        else -> bankData.accountType
                    }
                }

                // Detectar tipos de cuenta basados en palabras clave
                normalizedLine.contains("cuenta") || normalizedLine.contains("cta") -> {
                    when {
                        normalizedLine.contains("rut") -> bankData.accountType = "Cuenta RUT"
                        normalizedLine.contains("vista") -> bankData.accountType = "Cuenta Vista"
                        normalizedLine.contains("corriente") -> bankData.accountType = "Cuenta Corriente"
                        normalizedLine.contains("electronica") -> bankData.accountType = "Chequera Electrónica"
                        normalizedLine.contains("chequera") -> bankData.accountType = "Chequera Electrónica"
                        normalizedLine.contains("ahorro") -> bankData.accountType = "Cuenta de Ahorro"
                    }
                }

                // Detectar número de cuenta (7-20 dígitos)
                line.matches(Regex(".*\\d{7,20}.*")) &&
                        !line.contains("rut", true) &&
                        bankData.accountNumber == null -> {
                    bankData.accountNumber = line.replace(Regex("[^0-9]"), "")
                }

                // Detectar RUT (formato xx.xxx.xxx-x o xxxxxxxx-x)
                line.matches(Regex(".*\\b\\d{1,2}\\.?\\d{3}\\.?\\d{3}-\\d\\b.*")) -> {
                    bankData.rut = line.replace(Regex("[^\\dxX.-]"), "").trim()
                }

                // Detectar correos electrónicos
                line.contains("@") && line.matches(Regex("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}")) -> {
                    bankData.email = line.trim()
                }

                // Detectar nombres de empresa o persona
                (line.contains("ltda", true) ||
                        line.contains("s.a", true) ||
                        line.contains("spa", true) ||
                        (line.length > 5 && !line.contains("@") &&
                                !line.matches(Regex(".*\\d{7,20}.*")))) -> {
                    if (bankData.companyName == null) {
                        bankData.companyName = line.trim()
                    }
                }
            }
        }

        return bankData
    }


    private fun ImageProxy.toBitmap(): Bitmap? {
        val yBuffer = planes[0].buffer
        val uBuffer = planes[1].buffer
        val vBuffer = planes[2].buffer

        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()

        val nv21 = ByteArray(ySize + uSize + vSize)

        yBuffer.get(nv21, 0, ySize)
        vBuffer.get(nv21, ySize, vSize)
        uBuffer.get(nv21, ySize + vSize, uSize)

        val yuvImage = YuvImage(nv21, ImageFormat.NV21, width, height, null)
        val out = ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(0, 0, width, height), 100, out)
        val imageBytes = out.toByteArray()
        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
    }

    private fun cropBitmap(bitmap: Bitmap, rotation: Int): Bitmap? {
        return try {
            val (cropX, cropY, cropWidth, cropHeight) = when (rotation) {
                90, 270 -> {
                    val x = (scanRegion.top * bitmap.width).roundToInt()
                    val y = (scanRegion.left * bitmap.height).roundToInt()
                    val width = ((scanRegion.bottom - scanRegion.top) * bitmap.width).roundToInt()
                    val height = ((scanRegion.right - scanRegion.left) * bitmap.height).roundToInt()
                    listOf(x, y, width, height)
                }
                else -> {
                    val x = (scanRegion.left * bitmap.width).roundToInt()
                    val y = (scanRegion.top * bitmap.height).roundToInt()
                    val width = ((scanRegion.right - scanRegion.left) * bitmap.width).roundToInt()
                    val height = ((scanRegion.bottom - scanRegion.top) * bitmap.height).roundToInt()
                    listOf(x, y, width, height)
                }
            }

            Bitmap.createBitmap(bitmap, cropX, cropY, cropWidth, cropHeight)
        } catch (e: Exception) {
            Log.e(TAG, "Error al recortar la imagen: ${e.message}")
            null
        }
    }

    private fun toggleFlash() {
        camera?.cameraControl?.enableTorch(!isFlashOn)
    }

    private fun updateFlashIcon() {
        binding.btnFlashIcon.setImageResource(
            if (isFlashOn) R.drawable.flashlight else R.drawable.flashlight_off
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    companion object {
        private const val TAG = "CameraActivity"
    }
}