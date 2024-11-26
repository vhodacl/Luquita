package com.vhoda.luquita

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.IOException
import android.graphics.ImageDecoder
import android.os.Build
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.cardview.widget.CardView
import androidx.core.view.isVisible
import android.graphics.Color

class CheckInImageActivity : AppCompatActivity() {
    private val recognizer by lazy { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }

    private lateinit var progressBar: ProgressBar
    private lateinit var imageView: ImageView
    private lateinit var messageTextView: TextView
    private lateinit var btnFinalizar: Button
    private lateinit var cardView: CardView
    private lateinit var scanFrameContainer: ConstraintLayout

    private var isProcessing = false
    private var currentImageUri: Uri? = null
    private var isAdjustingFrame = true

    private var detectedText: String = ""  // Variable para almacenar el texto detectado

    companion object {
        private const val TAG = "CheckInImageActivity"
        private const val MIN_MARGIN = 0
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_check_in_image)

        setupTransparentBars()
        initializeViews()
        handleIncomingImage()
        btnFinalizar.setOnClickListener { finishAndSendResults() }
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

    private fun initializeViews() {
        progressBar = findViewById(R.id.progressBar)
        imageView = findViewById<ImageView>(R.id.iv_selected_image).apply {
            scaleType = ImageView.ScaleType.FIT_CENTER
            visibility = View.VISIBLE
        }
        messageTextView = findViewById(R.id.tv_checkin_message)
        btnFinalizar = findViewById(R.id.btn_finalizar)
        cardView = findViewById(R.id.card_view)
        scanFrameContainer = findViewById(R.id.scan_frame_container)
    }

    private fun handleIncomingImage() {
        intent.getStringExtra("image_uri")?.let { uriString ->
            Log.d(TAG, "Received URI: $uriString")

            try {
                currentImageUri = Uri.parse(uriString)
                currentImageUri?.let {
                    displayImage(it)
                    processImage(it) // Procesa la imagen inmediatamente.
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error al procesar URI: ${e.message}")
                showError("Error al cargar la imagen")
            }
        } ?: showError("No se recibió la imagen")
    }

    private fun displayImage(uri: Uri) {
        try {
            val bitmap = getBitmapFromUri(uri)
            bitmap?.let {
                imageView.setImageBitmap(it)
                imageView.visibility = View.VISIBLE
            } ?: throw IOException("No se pudo crear el bitmap")
        } catch (e: Exception) {
            Log.e(TAG, "Error al mostrar la imagen: ${e.message}")
            showError("Error al mostrar la imagen")
        }
    }

    private fun processImage(imageUri: Uri) {
        if (isProcessing) {
            Log.w(TAG, "Procesamiento en curso")
            return
        }

        isProcessing = true
        showProgress(true)

        try {
            val bitmap = getBitmapFromUri(imageUri) ?: throw IOException("No se pudo crear el bitmap")
            val image = InputImage.fromBitmap(bitmap, 0)

            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    val recognizedText = visionText.text
                    Log.d(TAG, "Texto reconocido: $recognizedText")

                    // Usar el mismo parser que CameraActivity
                    val bankData = parseBankData(recognizedText)
                    
                    // Formatear el texto detectado con los datos parseados
                    detectedText = buildFormattedText(bankData)

                    val tvDetectedText = findViewById<TextView>(R.id.tv_detected_text)
                    tvDetectedText.text = detectedText

                    val tvCheckinMessage = findViewById<TextView>(R.id.tv_checkin_message)
                    tvCheckinMessage.visibility = View.VISIBLE

                    val ivProcessedImage = findViewById<ImageView>(R.id.iv_processed_image)
                    ivProcessedImage.setImageBitmap(bitmap)

                    toggleScanMode(false)
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error en OCR: ${e.message}")
                    messageTextView.text = "Error al procesar la imagen"
                    showError("Error al procesar la imagen: ${e.message}")
                }
                .addOnCompleteListener {
                    isProcessing = false
                    showProgress(false)
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error inesperado: ${e.message}")
            messageTextView.text = "Error inesperado"
            showError("Error inesperado: ${e.message}")
            isProcessing = false
            showProgress(false)
        }
    }

    private fun parseBankData(text: String): BankData {
        val bankData = BankData()
        val lines = text.split("\n")

        // Mapa de palabras clave para detectar bancos (igual que en CameraActivity)
        val bankKeywords = mapOf(
            "Banco BCI" to listOf("bci"),
            "Banco BCI/MACH" to listOf("mach"),
            "Banco BICE" to listOf("bice"),
            "Banco Corpbanca" to listOf("corpbanca", "itau"),
            "Banco de Chile" to listOf("chile"),
            "Banco Estado" to listOf("estado", "banestado"),
            "Banco Falabella" to listOf("falabella"),
            "Banco Internacional" to listOf("internacional"),
            "Banco Ripley" to listOf("ripley"),
            "Banco Santander" to listOf("santander"),
            "Banco Security" to listOf("security"),
            "Consorcio" to listOf("consorcio"),
            "Coopeuch" to listOf("coopeuch"),
            "Copec APP" to listOf("copec"),
            "Itaú" to listOf("itau"),
            "Lapolar Prepago" to listOf("polar", "lapolar"),
            "Mercado Pago" to listOf("mercado", "mercadopago", "mp"),
            "Scotiabank" to listOf("scotia", "scotiabank"),
            "TAPP" to listOf("tapp"),
            "Tenpo" to listOf("tenpo")
        )

        // Procesar cada línea
        for (line in lines) {
            val normalizedLine = line.trim().lowercase()

            // Detectar banco usando palabras clave
            if (bankData.bank == null || bankData.bank == "No disponible") {
                for ((bankName, keywords) in bankKeywords) {
                    if (keywords.any { keyword -> normalizedLine.contains(keyword.lowercase()) }) {
                        bankData.bank = bankName
                        break
                    }
                }
            }

            when {
                // Detectar RUT
                line.matches(Regex(".*\\b\\d{1,2}[.]?\\d{3}[.]?\\d{3}-?[\\dkK]\\b.*")) -> {
                    val rutMatch = line.replace(Regex("[^\\dkK.-]"), "").trim()
                    bankData.rut = formatRut(rutMatch)
                }

                // Detectar tipos de cuenta
                normalizedLine.contains("cuenta") || normalizedLine.contains("cta") -> {
                    when {
                        normalizedLine.contains("rut") -> {
                            bankData.accountType = "Cuenta RUT"
                            bankData.bank = "Banco Estado"
                            // Si ya tenemos el RUT, usarlo como número de cuenta
                            if (bankData.rut != null) {
                                bankData.accountNumber = bankData.rut!!.replace(Regex("[^0-9]"), "").dropLast(1)
                            }
                        }
                        normalizedLine.contains("vista") -> bankData.accountType = "Cuenta Vista"
                        normalizedLine.contains("corriente") -> bankData.accountType = "Cuenta Corriente"
                        normalizedLine.contains("electronica") || normalizedLine.contains("chequera") -> 
                            bankData.accountType = "Chequera Electrónica"
                        normalizedLine.contains("ahorro") -> bankData.accountType = "Cuenta de Ahorro"
                    }
                }

                // Detectar número de cuenta (si no es Cuenta RUT)
                line.matches(Regex(".*\\d{7,20}.*")) && 
                !line.contains("rut", true) && 
                bankData.accountType != "Cuenta RUT" -> {
                    bankData.accountNumber = line.replace(Regex("[^0-9]"), "")
                }

                // Detectar correo electrónico
                line.contains("@") -> {
                    val emailMatch = line.trim().split("\\s+".toRegex())
                        .find { it.contains("@") && it.matches(Regex(".*@.*\\.[a-zA-Z]{2,}")) }
                    if (emailMatch != null) {
                        bankData.email = emailMatch
                    }
                }

                // Detectar nombre de empresa o persona (si no es una línea con datos bancarios)
                line.length > 5 && 
                !line.contains("@") && 
                !line.matches(Regex(".*\\d{7,20}.*")) && 
                !normalizedLine.contains("cuenta") && 
                !normalizedLine.contains("banco") && 
                bankData.companyName == null -> {
                    bankData.companyName = line.trim()
                }
            }
        }

        return bankData
    }

    private fun buildFormattedText(bankData: BankData): String {
        return buildString {
            appendLine("Nombre: ${bankData.companyName ?: "No disponible"}")
            appendLine("RUT: ${bankData.rut ?: "No disponible"}")
            appendLine("Banco: ${bankData.bank ?: "No disponible"}")
            appendLine("Tipo de Cuenta: ${bankData.accountType ?: "No disponible"}")
            appendLine("Número de Cuenta: ${bankData.accountNumber ?: "No disponible"}")
            appendLine("Correo: ${bankData.email ?: "No disponible"}")
        }.trim()
    }

    private fun finishAndSendResults() {
        val intent = Intent(this, ResultActivity::class.java)
        intent.putExtra("detected_text", detectedText)
        currentImageUri?.let {
            intent.putExtra("image_uri", it.toString())
        }
        startActivity(intent)
        finish()
    }

    private fun getBitmapFromUri(uri: Uri): Bitmap? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(contentResolver, uri)
                ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                    decoder.isMutableRequired = true
                }
            } else {
                @Suppress("DEPRECATION")
                MediaStore.Images.Media.getBitmap(contentResolver, uri)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al obtener bitmap: ${e.message}")
            null
        }
    }

    private fun showProgress(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        Log.e(TAG, message)
    }

    private fun toggleScanMode(showScanFrame: Boolean) {
        scanFrameContainer.isVisible = showScanFrame
        cardView.isVisible = !showScanFrame
        btnFinalizar.text = if (showScanFrame) "Procesar" else "Finalizar"
        isAdjustingFrame = showScanFrame
    }

    private fun formatRut(rut: String): String {
        val cleanRut = rut.replace(Regex("[^0-9Kk]"), "").uppercase()
        return when {
            cleanRut.length <= 1 -> cleanRut
            else -> {
                val body = cleanRut.substring(0, cleanRut.length - 1)
                val dv = cleanRut.last()
                "${body.reversed().chunked(3).joinToString(".").reversed()}-$dv"
            }
        }
    }
}
