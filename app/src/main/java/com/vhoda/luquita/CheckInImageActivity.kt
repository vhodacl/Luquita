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

    // Variables para el manejo del marco de escaneo
    private var activeCorner: View? = null
    private var initialTouchX: Float = 0f
    private var initialTouchY: Float = 0f
    private var corners: List<View> = emptyList()

    companion object {
        private const val TAG = "CheckInImageActivity"
        private const val MIN_MARGIN = 0
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_check_in_image)

        initializeViews()
        handleIncomingImage() // Procesa automáticamente al cargar la imagen.
        btnFinalizar.setOnClickListener { finish() } // Botón para finalizar la actividad.
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

    private fun setupScanFrame() {
        corners = listOf(
            R.id.corner_top_left,
            R.id.corner_top_right,
            R.id.corner_bottom_left,
            R.id.corner_bottom_right
        ).map { findViewById(it) }

        corners.forEach { corner ->
            corner.setOnTouchListener { view, event ->
                handleCornerTouch(view, event)
            }
        }
    }

    private fun handleCornerTouch(view: View, event: MotionEvent): Boolean {
        if (!isAdjustingFrame) return false

        return when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                activeCorner = view
                initialTouchX = event.rawX
                initialTouchY = event.rawY
                true
            }
            MotionEvent.ACTION_MOVE -> {
                if (activeCorner == view) {
                    val deltaX = event.rawX - initialTouchX
                    val deltaY = event.rawY - initialTouchY
                    updateCornerPosition(view, deltaX, deltaY)
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                } else false
            }
            MotionEvent.ACTION_UP -> {
                activeCorner = null
                true
            }
            else -> false
        }
    }

    private fun setupButtons() {
        btnFinalizar.setOnClickListener {
            if (isAdjustingFrame) {
                processImageWithFrame()
            } else {
                finish()
            }
        }
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


    private fun updateCornerPosition(corner: View, deltaX: Float, deltaY: Float) {
        val layoutParams = corner.layoutParams as ConstraintLayout.LayoutParams
        val maxMarginHorizontal = scanFrameContainer.width - corner.width
        val maxMarginVertical = scanFrameContainer.height - corner.height

        when (corner.id) {
            R.id.corner_top_left -> {
                layoutParams.leftMargin = (layoutParams.leftMargin + deltaX.toInt())
                    .coerceIn(MIN_MARGIN, maxMarginHorizontal)
                layoutParams.topMargin = (layoutParams.topMargin + deltaY.toInt())
                    .coerceIn(MIN_MARGIN, maxMarginVertical)
            }
            R.id.corner_top_right -> {
                layoutParams.rightMargin = (layoutParams.rightMargin - deltaX.toInt())
                    .coerceIn(MIN_MARGIN, maxMarginHorizontal)
                layoutParams.topMargin = (layoutParams.topMargin + deltaY.toInt())
                    .coerceIn(MIN_MARGIN, maxMarginVertical)
            }
            R.id.corner_bottom_left -> {
                layoutParams.leftMargin = (layoutParams.leftMargin + deltaX.toInt())
                    .coerceIn(MIN_MARGIN, maxMarginHorizontal)
                layoutParams.bottomMargin = (layoutParams.bottomMargin - deltaY.toInt())
                    .coerceIn(MIN_MARGIN, maxMarginVertical)
            }
            R.id.corner_bottom_right -> {
                layoutParams.rightMargin = (layoutParams.rightMargin - deltaX.toInt())
                    .coerceIn(MIN_MARGIN, maxMarginHorizontal)
                layoutParams.bottomMargin = (layoutParams.bottomMargin - deltaY.toInt())
                    .coerceIn(MIN_MARGIN, maxMarginVertical)
            }
        }

        corner.layoutParams = layoutParams
    }

    private fun toggleScanMode(showScanFrame: Boolean) {
        scanFrameContainer.isVisible = showScanFrame
        cardView.isVisible = !showScanFrame
        btnFinalizar.text = if (showScanFrame) "Procesar" else "Finalizar"
        isAdjustingFrame = showScanFrame
    }

    private fun processImageWithFrame() {
        currentImageUri?.let { uri ->
            toggleScanMode(false)
            processImage(uri)
        }
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

        // Eliminar el mensaje "Procesando imagen..."
        // messageTextView.text = "Procesando imagen..." // Ya no es necesario

        try {
            val bitmap = getBitmapFromUri(imageUri) ?: throw IOException("No se pudo crear el bitmap")
            val image = InputImage.fromBitmap(bitmap, 0)

            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    val recognizedText = visionText.text
                    Log.d(TAG, "Texto reconocido: $recognizedText") // Agregar log para ver lo que se reconoce

                    // Verificar si el texto reconocido no está vacío
                    val detectedText = if (recognizedText.isNotEmpty()) {
                        recognizedText
                    } else {
                        "No se detectó texto en la imagen"
                    }

                    // Agregar un log para verificar el texto antes de actualizar el TextView
                    Log.d(TAG, "Texto detectado: $detectedText")

                    // Actualizar el TextView con el texto detectado
                    val tvDetectedText = findViewById<TextView>(R.id.tv_detected_text)
                    tvDetectedText.text = detectedText

                    // Asegurarse de que el título "Texto Detectado" sea visible
                    val tvCheckinMessage = findViewById<TextView>(R.id.tv_checkin_message)
                    tvCheckinMessage.visibility = View.VISIBLE  // Hacer visible el título

                    // Mostrar la imagen procesada
                    val ivProcessedImage = findViewById<ImageView>(R.id.iv_processed_image)
                    ivProcessedImage.setImageBitmap(bitmap) // Muestra la imagen procesada

                    toggleScanMode(false) // Mostrar el CardView con resultados.
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
}