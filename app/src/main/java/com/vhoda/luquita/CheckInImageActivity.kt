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

    private val bankKeywords = mapOf(
        "Banco Estado" to listOf("estado", "bancoestado", "banco estado"),
        "Banco Santander" to listOf("santander"),
        "Banco de Chile" to listOf("chile", "banco de chile", "bancochile"),
        "Banco Falabella" to listOf("falabella"),
        "Banco Ripley" to listOf("ripley"),
        "BCI" to listOf("bci"),
        "Banco BICE" to listOf("bice"),
        "Banco Corpbanca" to listOf("corpbanca", "itau", "itaú"),
        "Banco Internacional" to listOf("internacional"),
        "Banco Security" to listOf("security"),
        "Consorcio" to listOf("consorcio"),
        "Coopeuch" to listOf("coopeuch"),
        "Mercado Pago" to listOf("mercadopago", "mercado pago", "mp"),
        "Lapolar Prepago" to listOf("lapolar", "la polar"),
        "Scotiabank" to listOf("scotia", "scotiabank"),
        "Itaú" to listOf("itau", "itaú"),
        "Tenpo" to listOf("tenpo"),
        "TAPP" to listOf("tapp"),
        "Copec APP" to listOf("copec"),
        "BCI/MACH" to listOf("mach")
    )

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
        val lines = text.split("\n").map { it.trim() }
        
        // Lista completa de palabras a excluir
        val excludeWords = setOf(
            // Palabras comunes de transferencia
            "datos", "transferir", "transferencia", "gracias", "muchas",
            "enviar", "comprobante", "correo", "asunto", "indicar",
            "porfavor", "favor", "disponibles", "citas",
            
            // Palabras bancarias
            "banco", "cuenta", "cta", "rut", "corriente", "vista",
            "num", "numero", "número", "transferencia",
            
            // Instrucciones y mensajes
            "debe", "enviar", "mandar", "adjuntar", "comprobante",
            "pago", "pagos", "depositar", "deposito", "depósito",
            "disponible", "disponibles", "citas", "paciente",
            "mes", "asunto", "indicar", "nombre",
            
            // Títulos y encabezados
            "datos", "para", "transferir", "transferencia",
            
            // Otros
            "+569", "whatsapp", "contacto", "tel", "teléfono",
            "celular", "móvil", "movil", "información", "info"
        )

        // Primera pasada: detectar banco y tipo de cuenta
        lines.forEach { line ->
            val lowerLine = line.lowercase()
            
            // Detectar banco y tipo de cuenta especial (Cuenta RUT)
            if (lowerLine.contains("cta rut") || lowerLine.contains("cuenta rut")) {
                bankData.bank = "Banco Estado"
                bankData.accountType = "Cuenta RUT"
            } 
            // Detectar banco explícito
            else if (lowerLine.contains("banco")) {
                when {
                    lowerLine.contains("estado") -> bankData.bank = "Banco Estado"
                    lowerLine.contains("santander") -> bankData.bank = "Banco Santander"
                    lowerLine.contains("chile") -> bankData.bank = "Banco de Chile"
                    lowerLine.contains("falabella") -> bankData.bank = "Banco Falabella"
                    lowerLine.contains("ripley") -> bankData.bank = "Banco Ripley"
                    lowerLine.contains("bci") -> bankData.bank = "BCI"
                    lowerLine.contains("bice") -> bankData.bank = "Banco BICE"
                    lowerLine.contains("corpbanca") -> bankData.bank = "Banco Corpbanca"
                    lowerLine.contains("internacional") -> bankData.bank = "Banco Internacional"
                    lowerLine.contains("security") -> bankData.bank = "Banco Security"
                }
            }
            
            // Detectar tipo de cuenta (solo si no es Cuenta RUT)
            if (bankData.accountType != "Cuenta RUT" && 
                (lowerLine.contains("cuenta") || lowerLine.contains("cta"))) {
                when {
                    lowerLine.contains("corriente") -> bankData.accountType = "Cuenta Corriente"
                    lowerLine.contains("vista") -> bankData.accountType = "Cuenta Vista"
                    lowerLine.contains("ahorro") -> bankData.accountType = "Cuenta Ahorro"
                }
            }
            
            // Detectar otros servicios financieros (solo si no se ha detectado un banco)
            if (bankData.bank == null) {
                val words = line.split(" ")
                when {
                    words.any { it.equals("consorcio", ignoreCase = true) } -> bankData.bank = "Consorcio"
                    words.any { it.equals("coopeuch", ignoreCase = true) } -> bankData.bank = "Coopeuch"
                    words.any { it.equals("scotia", ignoreCase = true) } -> bankData.bank = "Scotiabank"
                    words.any { it.equals("itau", ignoreCase = true) || it.equals("itaú", ignoreCase = true) } -> bankData.bank = "Itaú"
                    words.any { it.equals("tenpo", ignoreCase = true) } -> bankData.bank = "Tenpo"
                    words.any { it.equals("tapp", ignoreCase = true) } -> bankData.bank = "TAPP"
                    words.any { it.equals("copec", ignoreCase = true) } -> bankData.bank = "Copec APP"
                    words.any { it.equals("mach", ignoreCase = true) } -> bankData.bank = "BCI/MACH"
                    lowerLine == "mercado pago" || lowerLine == "mercadopago" -> bankData.bank = "Mercado Pago"
                    lowerLine == "la polar" || lowerLine == "lapolar" -> bankData.bank = "Lapolar Prepago"
                }
            }
        }

        // Segunda pasada: procesar resto de datos
        lines.forEach { line ->
            val lowerLine = line.lowercase()
            when {
                // RUT con formato chileno
                lowerLine.matches(Regex(".*(\\d{1,2}[.]\\d{3}[.]\\d{3}-[\\dkK]|\\d{7,8}-[\\dkK]).*")) -> {
                    val rutMatch = line.replace(Regex("[^\\dkK.-]"), "")
                    bankData.rut = formatRut(rutMatch)
                    if (bankData.accountType == "Cuenta RUT") {
                        bankData.accountNumber = rutMatch.replace(Regex("[^0-9]"), "").dropLast(1)
                    }
                }

                // Email válido
                lowerLine.contains("@") && 
                !lowerLine.contains("asunto") && 
                !lowerLine.contains("comprobante") -> {
                    val emailRegex = Regex("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}")
                    emailRegex.find(line)?.let { 
                        bankData.email = it.value.lowercase()
                    }
                }

                // Número de cuenta
                line.matches(Regex(".*\\d{9,}.*")) && !line.contains("-") -> {
                    if (bankData.accountType == "Cuenta RUT") {
                        val rutNumbers = bankData.rut?.replace(Regex("[^0-9]"), "")?.dropLast(1)
                        if (rutNumbers != null && line.contains(rutNumbers)) {
                            bankData.accountNumber = rutNumbers
                        }
                    } else {
                        bankData.accountNumber = line.replace(Regex("[^0-9]"), "")
                    }
                }

                // Nombre (mejorado)
                else -> {
                    val words = line.split(" ")
                    if (words.size >= 2 && // Al menos nombre y apellido
                        !lowerLine.contains(Regex("[0-9@]")) && // Sin números ni @
                        !words.any { word -> excludeWords.contains(word.lowercase()) } && // Sin palabras excluidas
                        !lowerLine.contains("banco") && // Verificaciones adicionales
                        bankData.companyName.isNullOrBlank()) {
                        
                        bankData.companyName = words.joinToString(" ") { word ->
                            word.capitalize()
                        }
                    }
                }
            }
        }

        // Verificaciones finales
        if (bankData.accountType == "Cuenta RUT") {
            bankData.bank = "Banco Estado"
        }

        return bankData
    }

    private fun buildFormattedText(bankData: BankData): String {
        return buildString {
            appendLine(bankData.companyName?.trim() ?: "No disponible")
            appendLine(bankData.rut?.trim() ?: "No disponible")
            appendLine(bankData.bank?.trim() ?: "No disponible")
            appendLine(bankData.accountType?.trim() ?: "No disponible")
            appendLine(bankData.accountNumber?.trim() ?: "No disponible")
            appendLine(bankData.email?.trim() ?: "No disponible")
        }.trim()
    }

    private fun finishAndSendResults() {
        val intent = Intent(this, ResultActivity::class.java).apply {
            putExtra(ResultActivity.EXTRA_DETECTED_TEXT, detectedText)
            putExtra("FROM_CHECKINIMAGE", true)  // Agregar flag para indicar el origen
            currentImageUri?.let {
                putExtra("image_uri", it.toString())
            }
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

    // Función auxiliar para capitalizar strings
    private fun String.capitalize(): String {
        return this.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }
}
