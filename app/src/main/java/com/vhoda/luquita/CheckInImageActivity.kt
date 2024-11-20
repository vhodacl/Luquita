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

        initializeViews()
        handleIncomingImage() // Procesa automáticamente al cargar la imagen.
        btnFinalizar.setOnClickListener { finishAndSendResults() } // Botón para finalizar la actividad y enviar los datos
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

                    detectedText = if (recognizedText.isNotEmpty()) {
                        recognizedText
                    } else {
                        "No se detectó texto en la imagen"
                    }

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
}
