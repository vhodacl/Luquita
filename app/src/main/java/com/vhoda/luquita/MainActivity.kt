package com.vhoda.luquita

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.vhoda.luquita.databinding.ActivityMainBinding
import android.app.AlertDialog
import android.view.LayoutInflater
import java.io.IOException

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val PICK_IMAGE = 101
    private val TAKE_PHOTO = 102
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private lateinit var loadingDialog: AlertDialog
    private val REQUIRED_PERMISSIONS = arrayOf(
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.CAMERA
    )
    private val REQUEST_CODE = 123

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initLoadingDialog()
        checkPermissions()
        setupClickListeners()
    }

    private fun initLoadingDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_loading, null)
        loadingDialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()
    }

    private fun setupClickListeners() {
        binding.gallery.setOnClickListener {
            // Remove permission check for gallery
            openGallery()
        }

        binding.camera.setOnClickListener {
            if (hasCameraPermission()) {
                openCamera()
            } else {
                requestPermissions()
            }
        }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, PICK_IMAGE)
    }
    private fun openCamera() {
        val intent = Intent(this, CameraActivity::class.java)
        startActivityForResult(intent, TAKE_PHOTO)
    }

    private fun checkPermissions() {
        if (!hasRequiredPermissions()) {
            requestPermissions()
        }
    }

    private fun hasRequiredPermissions(): Boolean {
        return REQUIRED_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun hasStoragePermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun hasCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == RESULT_OK && data != null) {
            when (requestCode) {
                PICK_IMAGE -> handleGalleryResult(data)
                TAKE_PHOTO -> handleCameraResult(data)
            }
        }
    }

    private fun handleGalleryResult(data: Intent) {
        try {
            val imageUri: Uri? = data.data
            if (imageUri != null) {
                val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, imageUri)
                processImage(bitmap)
            } else {
                showError("No se pudo obtener la imagen")
            }
        } catch (e: IOException) {
            Log.e("OCR", "Error al cargar la imagen: ${e.message}")
            showError("Error al cargar la imagen")
        }
    }

    private fun handleCameraResult(data: Intent) {
        val bitmap = data.getParcelableExtra<Bitmap>("photo")
        if (bitmap != null) {
            processImage(bitmap)
        } else {
            showError("No se pudo obtener la foto")
        }
    }

    private fun processImage(bitmap: Bitmap) {
        try {
            showLoading()
            val image = InputImage.fromBitmap(bitmap, 0)

            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    try {
                        hideLoading()
                        val recognizedText = visionText.text
                        Log.d("OCR", "Texto reconocido: $recognizedText")

                        val bankData = BankDataParser.parse(recognizedText)
                        Log.d("OCR", "Datos bancarios: ${bankData.toMap()}")

                        if (bankData.isValid()) {
                            showResults(recognizedText, bankData)
                        } else {
                            showMissingFieldsError(bankData)
                        }
                    } catch (e: Exception) {
                        Log.e("OCR", "Error al procesar datos: ${e.message}")
                        showError("Error al procesar los datos")
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("OCR", "Error en OCR: ${e.message}")
                    hideLoading()
                    showError("Error al procesar la imagen")
                }
        } catch (e: Exception) {
            Log.e("OCR", "Error al iniciar proceso: ${e.message}")
            hideLoading()
            showError("Error al iniciar el proceso")
        }
    }

    private fun showResults(recognizedText: String, bankData: BankData) {
        val intent = Intent(this, ResultActivity::class.java).apply {
            putExtra(ResultActivity.EXTRA_DETECTED_TEXT, recognizedText)
            putExtra(ResultActivity.EXTRA_BANK_DATA, bankData.toMap().toString())
        }
        startActivity(intent)
    }

    private fun showMissingFieldsError(bankData: BankData) {
        val missingFields = getMissingFields(bankData)
        Toast.makeText(
            this,
            "Faltan datos requeridos: $missingFields",
            Toast.LENGTH_LONG
        ).show()
    }

    private fun showLoading() {
        if (!loadingDialog.isShowing) {
            loadingDialog.show()
        }
    }

    private fun hideLoading() {
        if (loadingDialog.isShowing) {
            loadingDialog.dismiss()
        }
    }

    private fun showError(message: String) {
        hideLoading()
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun getMissingFields(bankData: BankData): String {
        return buildList {
            if (bankData.rut == null) add("RUT")
            if (bankData.accountNumber == null) add("Número de cuenta")
            if (bankData.bank == null) add("Banco")
            if (bankData.accountType == null) add("Tipo de cuenta")
        }.joinToString(", ")
    }
}

// BankData.kt
data class BankData(
    var rut: String? = null,
    var email: String? = null,
    var bank: String? = null,
    var accountType: String? = null,
    var accountNumber: String? = null,
    var companyName: String? = null
) {
    fun isValid(): Boolean {
        var validFields = 0
        if (!rut.isNullOrBlank() && isValidRut(rut!!)) validFields++
        if (!accountNumber.isNullOrBlank()) validFields++
        if (!bank.isNullOrBlank()) validFields++
        if (!accountType.isNullOrBlank()) validFields++
        if (!email.isNullOrBlank() && isValidEmail(email!!)) validFields++
        return validFields >= 3
    }

    private fun isValidRut(rut: String): Boolean {
        return rut.matches(Regex("\\d{1,2}\\.?\\d{3}\\.?\\d{3}-[\\dkK]"))
    }

    private fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    fun toMap(): Map<String, String> = mapOf(
        "RUT" to (rut ?: ""),
        "Email" to (email ?: ""),
        "Banco" to (bank ?: ""),
        "Tipo de Cuenta" to (accountType ?: ""),
        "Número de Cuenta" to (accountNumber ?: ""),
        "Nombre Empresa" to (companyName ?: "")
    )
}