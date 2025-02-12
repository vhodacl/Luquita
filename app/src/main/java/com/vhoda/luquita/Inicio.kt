package com.vhoda.luquita

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.vhoda.luquita.databinding.ActivityInicioBinding
import com.vhoda.luquita.model.TransferFact
import com.vhoda.luquita.model.TransferFactsProvider
import android.content.ClipboardManager
import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.GestureDetector
import com.google.android.material.card.MaterialCardView

class Inicio : AppCompatActivity() {
    private lateinit var binding: ActivityInicioBinding  // Cambio aquí
    private val PICK_IMAGE = 101
    private val REQUIRED_PERMISSIONS = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
    } else {
        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }
    private val REQUEST_CODE = 123
    private lateinit var cardSimple: MaterialCardView

    companion object {
        private const val TAG = "Inicio"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "Inicio onCreate")
        binding = ActivityInicioBinding.inflate(layoutInflater)  // Cambio aquí
        setContentView(binding.root)

        cardSimple = binding.cardSimple
        setupTransparentBars()
        setupClickListeners()
        showRandomFact()
        setupEasterEgg()
    }

    private fun isFirstTime(): Boolean {
        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        return prefs.getBoolean("is_first_time", true)
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
                requestStoragePermission()
            }
        }

        binding.camera.setOnClickListener {
            val intent = Intent(this, CameraActivity::class.java)
            startActivity(intent)
        }

        binding.moreOptions.setOnClickListener {
            showBottomSheet()
        }

        binding.pasteButton.setOnClickListener {
            val clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            if (clipboardManager.hasPrimaryClip()) {
                val clipData = clipboardManager.primaryClip
                val text = clipData?.getItemAt(0)?.text?.toString()
                if (!text.isNullOrEmpty()) {
                    val intent = Intent(this, PasteNormalizeActivity::class.java).apply {
                        putExtra("clipboard_text", text)
                    }
                    startActivity(intent)
                } else {
                    Toast.makeText(this, "El último texto copiado en portapapeles está normalizado.", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "El portapapeles está vacío", Toast.LENGTH_SHORT).show()
            }
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

    private fun requestStoragePermission() {
        if (shouldShowRequestPermissionRationale(REQUIRED_PERMISSIONS[0])) {
            Toast.makeText(
                this,
                "Necesitamos acceso a la galería para seleccionar imágenes",
                Toast.LENGTH_LONG
            ).show()
        }
        ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE)
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

    private fun showBottomSheet() {
        val bottomSheetView = layoutInflater.inflate(R.layout.bottom_sheet_layout, null)
        val bottomSheetDialog = BottomSheetDialog(this)
        bottomSheetDialog.setContentView(bottomSheetView)

        // Configurar la barra de navegación
        bottomSheetDialog.window?.apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                navigationBarColor = resources.getColor(android.R.color.transparent, theme)
                decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
            }
        }

        // Configurar click listeners para las opciones
        bottomSheetView.findViewById<LinearLayout>(R.id.how_it_works).setOnClickListener {
            startActivity(Intent(this, HowItWorksActivity::class.java))
            bottomSheetDialog.dismiss()
        }

        bottomSheetView.findViewById<LinearLayout>(R.id.privacy_policy).setOnClickListener {
            // Abrir política de privacidad
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://vhoda.cl/luquita/policy"))
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(this, "No se pudo abrir el enlace", Toast.LENGTH_SHORT).show()
            }
            bottomSheetDialog.dismiss()
        }

        bottomSheetView.findViewById<LinearLayout>(R.id.developer_page).setOnClickListener {
            // Abrir página del desarrollador
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://vhoda.cl"))
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(this, "No se pudo abrir el enlace", Toast.LENGTH_SHORT).show()
            }
            bottomSheetDialog.dismiss()
        }

        bottomSheetDialog.show()
    }

    private fun showRandomFact() {
        // Animar el botón
        binding.btnNextFact.animate()
            .rotation(binding.btnNextFact.rotation + 360f)
            .setDuration(500)
            .start()

        // Ocultar textos con fade out
        binding.textCardContent.animate().alpha(0f).setDuration(200).start()
        binding.textCardSource.animate()
            .alpha(0f)
            .setDuration(200)
            .withEndAction {
                // Mostrar skeleton
                binding.skeletonLayout.visibility = View.VISIBLE
                binding.textCardContent.visibility = View.INVISIBLE
                binding.textCardSource.visibility = View.INVISIBLE
                
                // Simular carga de datos
                binding.textCardContent.postDelayed({
                    // Obtener nuevo fact
                    val fact = TransferFactsProvider.getRandomFact()
                    
                    // Ocultar skeleton
                    binding.skeletonLayout.visibility = View.GONE
                    
                    // Actualizar textos
                    binding.textCardContent.text = fact.fact
                    binding.textCardSource.text = fact.source ?: ""
                    
                    // Mostrar textos con fade in
                    binding.textCardContent.alpha = 0f
                    binding.textCardSource.alpha = 0f
                    binding.textCardContent.visibility = View.VISIBLE
                    binding.textCardSource.visibility = View.VISIBLE
                    
                    binding.textCardContent.animate().alpha(1f).setDuration(200).start()
                    binding.textCardSource.animate().alpha(1f).setDuration(200).start()
                }, 500) // 1 segundo de delay
            }
            .start()
        
        binding.btnNextFact.setOnClickListener {
            showRandomFact()
        }
    }

    private fun setupEasterEgg() {
        val gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onLongPress(e: MotionEvent) {
                // Iniciar la vibración en diferentes intervalos
                val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

                // Patrón de vibración: [inicio, duración de vibración, pausa, duración de vibración, pausa, duración de vibración]
                val pattern = longArrayOf(0, 100, 200, 200, 300) // 5 elementos
                val amplitudes = intArrayOf(0, 5, 15, 20, 0) // 5 elementos

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val vibrationEffect = VibrationEffect.createWaveform(pattern, amplitudes, -1)
                    vibrator.vibrate(vibrationEffect)
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(pattern, -1)
                }

                // Mostrar un Toast con el mensaje "Mantenga sostenido..."
                Toast.makeText(this@Inicio, "Mantenga precionado...", Toast.LENGTH_SHORT).show()

                // Iniciar la nueva actividad después de 3 segundos
                cardSimple.postDelayed({
                    val intent = Intent(this@Inicio, EasterEggActivity::class.java)
                    startActivity(intent)
                }, 3000) // Mantener el tiempo de espera para iniciar la actividad
            }
        })

        cardSimple.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            false
        }
    }
}