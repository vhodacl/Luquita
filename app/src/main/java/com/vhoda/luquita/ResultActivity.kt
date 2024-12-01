package com.vhoda.luquita

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.vhoda.luquita.databinding.ActivityResultBinding
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import android.os.Build
import com.google.android.material.snackbar.Snackbar
import android.view.inputmethod.InputMethodManager
import android.view.inputmethod.EditorInfo
import android.view.KeyEvent
import android.content.Context
import android.graphics.Color
import android.text.Editable
import android.text.TextWatcher
import com.vhoda.luquita.Inicio
import android.view.LayoutInflater
import android.view.ViewGroup

class ResultActivity : AppCompatActivity() {
    private lateinit var binding: ActivityResultBinding

    // Lista completa de bancos disponibles
    private val banksList = listOf(
        "Seleccione un banco",
        "BCI",
        "BCI/MACH",
        "Banco BICE",
        "Banco Corpbanca",
        "Banco de Chile",
        "Banco Estado",
        "Banco Falabella",
        "Banco Internacional",
        "Banco Ripley",
        "Banco Santander",
        "Banco Security",
        "Consorcio",
        "Coopeuch",
        "Copec APP",
        "Itaú",
        "Lapolar Prepago",
        "Mercado Pago",
        "Scotiabank",
        "TAPP",
        "Tenpo"
    )

    // Lista de tipos de cuenta
    private val accountTypesList = listOf(
        "Seleccione tipo de cuenta",
        "Cuenta de Ahorro",
        "Cuenta Corriente",
        "Chequera Electrónica",
        "Cuenta Vista",
        "Cuenta RUT"
    )

    private val bankLogos = mapOf(
        "Banco Estado" to R.drawable.bancoestado,
        "Banco Santander" to R.drawable.santander,
        "Banco de Chile" to R.drawable.bancochile,
        "Banco Falabella" to R.drawable.bancofalabella,
        "Banco Ripley" to R.drawable.bancoripley,
        "BCI" to R.drawable.bci,
        "Banco BICE" to R.drawable.bancobice,
        "Banco Corpbanca" to R.drawable.bancocorpbanca,
        "Banco Internacional" to R.drawable.bancointernacional,
        "Banco Security" to R.drawable.bancosecurity,
        "Consorcio" to R.drawable.consorcio,
        "Coopeuch" to R.drawable.coopeuch,
        "Mercado Pago" to R.drawable.mercadopago,
        "Lapolar Prepago" to R.drawable.lapolar,
        "Scotiabank" to R.drawable.scotiabank,
        "Itaú" to R.drawable.itau,
        "Tenpo" to R.drawable.tenpo,
        "TAPP" to R.drawable.tapp,
        "Copec APP" to R.drawable.copec,
        "Banco BCI/MACH" to R.drawable.mach,
        "BCI" to R.drawable.bci
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Verificar si la actividad anterior fue CheckInImageActivity
        val fromCheckInImage = intent.getBooleanExtra("FROM_CHECKINIMAGE", false)

        // Configurar las barras de estado y navegación (sin cambios)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.statusBarColor = resources.getColor(android.R.color.transparent, theme)
            window.navigationBarColor = resources.getColor(android.R.color.transparent, theme)
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        }

        binding = ActivityResultBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Obtener los datos desde el intent
        val detectedText = intent.getStringExtra(EXTRA_DETECTED_TEXT) ?: ""
        val bankDataString = intent.getStringExtra("BANK_DATA") ?: ""

        // Parsear los datos, usando los datos del banco si están disponibles
        val dataMap = if (bankDataString.isNotBlank()) {
            parseBankDataString(bankDataString)
        } else {
            parseDetectedData(detectedText)
        }

        // Si los datos no son válidos y no proviene de CheckInImageActivity, volvemos a la cámara
        if (!isDataValid(dataMap) && !fromCheckInImage) {
            startActivity(Intent(this, CameraActivity::class.java))
            finish()
            return
        }

        // Si los datos son válidos, continuar con la configuración de la UI
        setupUI(dataMap)
        setupSpinners(dataMap)
        setupInputBehavior()
    }

    private fun setupInputBehavior() {
        with(binding) {
            // Lista de EditTexts en orden
            val editTexts = listOf(
                companyNameEditText,
                rutEditText,
                emailEditText,
                accountNumberEditText
            )

            // Configurar cada EditText
            editTexts.forEachIndexed { index, editText ->
                editText.apply {
                    // Prevenir saltos de línea
                    setSingleLine()
                    
                    // Configurar acción del teclado
                    setOnEditorActionListener { _, actionId, event ->
                        if (actionId == EditorInfo.IME_ACTION_NEXT ||
                            (event?.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)) {
                            
                            // Si no es el último EditText, pasar al siguiente
                            if (index < editTexts.lastIndex) {
                                editTexts[index + 1].requestFocus()
                            } else {
                                // Si es el último, ocultar el teclado
                                val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                                imm.hideSoftInputFromWindow(windowToken, 0)
                            }
                            true
                        } else {
                            false
                        }
                    }
                }
            }
        }
    }

    private fun setupSpinners(dataMap: Map<String, String>) {
        // Crear un adaptador personalizado para el Spinner de bancos
        val bankAdapter = object : ArrayAdapter<String>(
            this,
            android.R.layout.simple_spinner_item, // Cambiamos a layout simple para vista cerrada
            banksList
        ) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                // Vista cuando el spinner está cerrado (solo texto)
                val view = super.getView(position, convertView, parent)
                (view as TextView).text = getItem(position)
                return view
            }

            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                // Vista cuando el spinner está abierto (logo + texto)
                val view = convertView ?: LayoutInflater.from(context)
                    .inflate(R.layout.spinner_item_bank, parent, false)
                
                val imageView = view.findViewById<ImageView>(R.id.bankLogo)
                val textView = view.findViewById<TextView>(R.id.bankName)
                
                val bankName = getItem(position)
                textView.text = bankName
                
                // Establecer el logo correspondiente
                val logoResource = if (position > 0) {
                    bankLogos[bankName] ?: R.drawable.nobank
                } else {
                    R.drawable.nobank
                }
                
                Glide.with(context)
                    .load(logoResource)
                    .apply(RequestOptions()
                        .transform(RoundedCorners(8))
                        .override(40, 40))
                    .into(imageView)

                return view
            }
        }

        binding.bankSpinner.apply {
            adapter = bankAdapter
            // Seleccionar el banco detectado si existe
            val detectedBank = dataMap["Banco"]
            val bankPosition = banksList.indexOf(detectedBank)
            setSelection(if (bankPosition != -1) bankPosition else 0)

            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    val isValid = position > 0
                    updateValidationIcon(binding.bankValidation, isValid)
                    
                    // Actualizar el logo principal cuando cambia la selección
                    val selectedBank = banksList[position]
                    val logoResource = if (position > 0) {
                        bankLogos[selectedBank] ?: R.drawable.nobank
                    } else {
                        R.drawable.nobank
                    }
                    
                    binding.logoImageView.apply {
                        visibility = View.VISIBLE
                        Glide.with(this@ResultActivity)
                            .load(logoResource)
                            .apply(RequestOptions().transform(RoundedCorners(15)))
                            .into(this)
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                    updateValidationIcon(binding.bankValidation, false)
                    binding.logoImageView.apply {
                        visibility = View.VISIBLE
                        Glide.with(this@ResultActivity)
                            .load(R.drawable.nobank)
                            .apply(RequestOptions().transform(RoundedCorners(15)))
                            .into(this)
                    }
                }
            }
        }

        // Configurar Spinner de Tipos de Cuenta
        val accountTypeAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            accountTypesList
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        binding.accountTypeSpinner.apply {
            adapter = accountTypeAdapter
            // Seleccionar el tipo de cuenta detectado si existe
            val detectedAccountType = dataMap["Tipo de Cuenta"]
            val accountTypePosition = accountTypesList.indexOf(detectedAccountType)
            setSelection(if (accountTypePosition != -1) accountTypePosition else 0)

            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    updateValidationIcon(binding.accountTypeValidation, position > 0)
                }
                override fun onNothingSelected(parent: AdapterView<*>?) {
                    updateValidationIcon(binding.accountTypeValidation, false)
                }
            }
        }
    }

    private fun setupUI(dataMap: Map<String, String>) {
        with(binding) {
            val fieldMappings = listOf(
                Triple(companyNameEditText, companyNameEditText.parent as View, "Nombre"),
                Triple(rutEditText, rutEditText.parent as View, "RUT"),
                Triple(emailEditText, emailEditText.parent as View, "Correo"),
                Triple(accountNumberEditText, accountNumberEditText.parent as View, "Número de Cuenta")
            )

            fieldMappings.forEach { (editText, parentView, key) ->
                setupField(
                    valueTextView = editText,
                    parentView = parentView,
                    value = dataMap[key],
                    key = key
                )
            }

            setupButtons(dataMap)
            configureBankLogo(dataMap)
        }
    }

    private fun setupField(valueTextView: EditText, parentView: View, value: String?, key: String) {
        valueTextView.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val text = s?.toString() ?: ""
                val isValid = when (key) {
                    "RUT" -> isValidRut(text)
                    "Correo" -> isValidEmail(text)
                    "Número de Cuenta" -> isValidAccountNumber(text)
                    else -> text.isNotBlank()
                }
                updateValidationIcon(
                    when (key) {
                        "Nombre" -> binding.companyNameValidation
                        "RUT" -> binding.rutValidation
                        "Correo" -> binding.emailValidation
                        "Número de Cuenta" -> binding.accountNumberValidation
                        else -> null
                    } ?: return,
                    isValid
                )
            }
        })

        // Configuración inicial del campo
        if (key == "RUT" && value?.contains(".") != true) {
            valueTextView.setText(if (value == "No disponible") "" else formatRut(value ?: ""))
        } else {
            valueTextView.setText(if (value == "No disponible") "" else value?.replace("\n", " "))
        }
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

    private fun configureBankLogo(dataMap: Map<String, String>) {
        val bankName = dataMap["Banco"]
        val logoResource = if (bankName == "Seleccione un banco" || bankName == null) {
            R.drawable.nobank
        } else {
            bankLogos[bankName] ?: R.drawable.nobank
        }

        binding.logoImageView.apply {
            Glide.with(this@ResultActivity)
                .load(logoResource)
                .apply(RequestOptions().transform(RoundedCorners(15)))
                .into(this)
            visibility = View.VISIBLE
        }
    }

    private fun setupButtons(dataMap: Map<String, String>) {
        with(binding) {
            val hasInvalidFields = dataMap.any { it.value == "No disponible" }

            retryButton.apply {
                visibility = if (hasInvalidFields) View.VISIBLE else View.GONE
                setOnClickListener {
                    if (hasInvalidFields) {
                        startActivity(Intent(this@ResultActivity, CameraActivity::class.java))
                        finish()
                    }
                }
            }

            doneButton.setOnClickListener {
                val intent = Intent(this@ResultActivity, Inicio::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
                finish()
            }

            copyAllCard.setOnClickListener {
                val allData = getAllData()
                copyToClipboard(allData)
                showCopiedIndicator()
            }

            shareCard.setOnClickListener {
                shareData(getAllData())
            }
        }
    }

    private fun getAllData(): String {
        with(binding) {
            return listOf(
                companyNameEditText.text.toString().takeIf { it.isNotBlank() } ?: "",
                rutEditText.text.toString().takeIf { it.isNotBlank() } ?: "",
                emailEditText.text.toString().takeIf { it.isNotBlank() } ?: "",
                bankSpinner.selectedItem.toString().takeIf { it != "Seleccione un banco" } ?: "",
                accountTypeSpinner.selectedItem.toString().takeIf { it != "Seleccione tipo de cuenta" } ?: "",
                accountNumberEditText.text.toString().takeIf { it.isNotBlank() } ?: ""
            ).filter { it.isNotEmpty() }.joinToString("\n")
        }
    }

    private fun setupField(
        valueTextView: TextView,
        parentView: View,
        value: String?,
        key: String
    ) {
        val finalValue = value?.takeIf { it.isNotBlank() } ?: "No disponible"

        valueTextView.apply {
            text = finalValue
            contentDescription = "$key: $finalValue"
        }

        (parentView as? LinearLayout)?.findViewById<ImageView>(android.R.id.empty)?.apply {
            val isValid = finalValue != "No disponible"
            setImageResource(if (isValid) R.drawable.task_alt else R.drawable.error)
            imageTintList = ContextCompat.getColorStateList(
                context,
                if (isValid) R.color.green else R.color.red
            )
            contentDescription = if (isValid) "Campo válido" else "Campo no disponible"
        }
    }

    private fun parseBankDataString(bankDataString: String): Map<String, String> {
        return try {
            bankDataString
                .trim('{', '}')
                .split(", ")
                .associate { pair ->
                    val (key, value) = pair.split("=")
                    key to (value.takeIf { it.isNotBlank() } ?: "No disponible")
                }
        } catch (e: Exception) {
            mapOf(
                "Nombre" to "No disponible",
                "RUT" to "No disponible",
                "Banco" to "No disponible",
                "Tipo de Cuenta" to "No disponible",
                "Número de Cuenta" to "No disponible",
                "Correo" to "No disponible"
            )
        }
    }

    private fun isDataValid(dataMap: Map<String, String>): Boolean {
        var validFields = 0
        dataMap.forEach { (_, value) ->
            if (value != "No disponible" && value.isNotBlank()) validFields++
        }
        return validFields >= 3
    }

    private fun parseDetectedData(detectedText: String): Map<String, String> {
        val dataMap = mutableMapOf(
            "Nombre" to "No disponible",
            "RUT" to "No disponible",
            "Banco" to "No disponible",
            "Tipo de Cuenta" to "No disponible",
            "Número de Cuenta" to "No disponible",
            "Correo" to "No disponible"
        )

        // Dividir el texto en líneas y asignar cada línea a su campo correspondiente
        val lines = detectedText.split("\n").map { it.trim() }
        if (lines.size >= 6) {
            // El texto viene en orden específico desde CheckInImageActivity
            dataMap["Nombre"] = lines[0].takeIf { it != "No disponible" } ?: "No disponible"
            dataMap["RUT"] = lines[1].takeIf { it != "No disponible" } ?: "No disponible"
            dataMap["Banco"] = lines[2].takeIf { it != "No disponible" } ?: "No disponible"
            dataMap["Tipo de Cuenta"] = lines[3].takeIf { it != "No disponible" } ?: "No disponible"
            dataMap["Número de Cuenta"] = lines[4].takeIf { it != "No disponible" } ?: "No disponible"
            dataMap["Correo"] = lines[5].takeIf { it != "No disponible" } ?: "No disponible"
        }

        return dataMap
    }

    private fun copyToClipboard(data: String) {
        val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Detected Data", data)
        clipboard.setPrimaryClip(clip)
    }

    private fun showCopiedIndicator() {
        Toast.makeText(
            this,
            "Copiado al portapapeles",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun updateValidationIcon(imageView: ImageView, isValid: Boolean) {
        imageView.isActivated = isValid
        imageView.setImageResource(if (isValid) R.drawable.task_alt else R.drawable.error)
        imageView.imageTintList = ContextCompat.getColorStateList(
            this,
            if (isValid) R.color.green else R.color.red
        )
    }

    // Funciones de validación
    private fun isValidRut(rut: String?): Boolean {
        if (rut.isNullOrBlank()) return false
        // Actualizado para aceptar K mayúscula o minúscula
        val pattern = Regex("^\\d{1,2}\\.\\d{3}\\.\\d{3}-[\\dKk]$")
        return pattern.matches(rut)
    }

    private fun isValidEmail(email: String?): Boolean {
        if (email.isNullOrBlank()) return false
        val pattern = Regex("^[A-Za-z0-9+_.-]+@(.+)$")
        return pattern.matches(email)
    }

    private fun isValidAccountNumber(accountNumber: String?): Boolean {
        if (accountNumber.isNullOrBlank()) return false
        // Validar que solo contenga números y tenga entre 7 y 20 dígitos
        val pattern = Regex("^\\d{7,20}$")
        return pattern.matches(accountNumber)
    }

    private fun shareData(data: String) {
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, data)
        }
        
        try {
            startActivity(Intent.createChooser(shareIntent, "Compartir mediante"))
        } catch (e: Exception) {
            Snackbar.make(
                binding.toolbar3,
                "No se encontraron aplicaciones para compartir",
                Snackbar.LENGTH_SHORT
            ).show()
        }
    }

    companion object {
        const val EXTRA_DETECTED_TEXT = "detected_text"
        const val EXTRA_BANK_DATA = "detected_text"
    }
}