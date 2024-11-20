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

class ResultActivity : AppCompatActivity() {
    private lateinit var binding: ActivityResultBinding

    // Lista completa de bancos disponibles
    private val banksList = listOf(
        "Seleccione un banco",
        "Banco Estado",
        "Banco Santander",
        "Banco de Chile",
        "Banco Falabella",
        "Banco Crédito e Inversiones",
        "Mercado Pago",
        "Scotiabank",
        "Itaú",
        "Tenpo",
        "TAPP",
        "Copec",
        "MACH"
    )

    // Lista de tipos de cuenta
    private val accountTypesList = listOf(
        "Seleccione tipo de cuenta",
        "Cuenta de Ahorro",
        "Cuenta Corriente",
        "Chequera Electrónica",
        "Cuenta Vista"
    )

    private val bankLogos = mapOf(
        "Banco Estado" to R.drawable.bancoestado,
        "Banco Santander" to R.drawable.santander,
        "Banco de Chile" to R.drawable.bancochile,
        "Banco Falabella" to R.drawable.bancofalabella,
        "Banco Crédito e Inversiones" to R.drawable.bci,
        "BCI" to R.drawable.bci,
        "Mercado Pago" to R.drawable.mercadopago,
        "Scotiabank" to R.drawable.scotiabank,
        "Itaú" to R.drawable.itau,
        "Tenpo" to R.drawable.tenpo,
        "TAPP" to R.drawable.tapp,
        "Copec" to R.drawable.copec,
        "MACH" to R.drawable.mach
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
    }


    private fun setupSpinners(dataMap: Map<String, String>) {
        // Configurar Spinner de Bancos
        val bankAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            banksList
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        binding.bankSpinner.apply {
            adapter = bankAdapter
            // Seleccionar el banco detectado si existe
            val detectedBank = dataMap["Banco"]
            val bankPosition = banksList.indexOf(detectedBank)
            setSelection(if (bankPosition != -1) bankPosition else 0)

            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    if (position > 0) {
                        val selectedBank = banksList[position]
                        configureBankLogo(mapOf("Banco" to selectedBank))
                    }
                }
                override fun onNothingSelected(parent: AdapterView<*>?) {}
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
        valueTextView.setText(value)
    }

    private fun configureBankLogo(dataMap: Map<String, String>) {
        val bankName = dataMap["Banco"]
        val logoResource = bankLogos[bankName]

        binding.logoImageView.apply {
            if (logoResource != null) {
                Glide.with(this@ResultActivity)
                    .load(logoResource)
                    .apply(RequestOptions().transform(RoundedCorners(15)))
                    .into(this)
                visibility = View.VISIBLE
            } else {
                visibility = View.GONE
            }
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
                val intent = Intent(this@ResultActivity, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
                finish()
            }

            copyAllCard.setOnClickListener {
                val allData = getAllData()
                copyToClipboard(allData)
                showCopiedIndicator()
            }
        }
    }

    private fun getAllData(): String {
        with(binding) {
            return listOf(
                companyNameEditText.text.toString(),
                rutEditText.text.toString(),
                emailEditText.text.toString(),
                bankSpinner.selectedItem.toString(),
                accountTypeSpinner.selectedItem.toString(),
                accountNumberEditText.text.toString()
            ).joinToString("\n")
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
            "Correo" to "No disponible",
            "Banco" to "No disponible",
            "Tipo de Cuenta" to "No disponible",
            "Número de Cuenta" to "No disponible"
        )

        detectedText.split("\n").forEach { line ->
            val trimmedLine = line.trim()
            when {
                trimmedLine.contains("banco", ignoreCase = true) -> {
                    when {
                        trimmedLine.contains("estado", ignoreCase = true) -> dataMap["Banco"] = "Banco Estado"
                        trimmedLine.contains("santander", ignoreCase = true) -> dataMap["Banco"] = "Banco Santander"
                        trimmedLine.contains("chile", ignoreCase = true) -> dataMap["Banco"] = "Banco de Chile"
                        trimmedLine.contains("falabella", ignoreCase = true) -> dataMap["Banco"] = "Banco Falabella"
                        trimmedLine.contains("bci", ignoreCase = true) -> dataMap["Banco"] = "BCI"
                        trimmedLine.contains("mercado pago", ignoreCase = true) -> dataMap["Banco"] = "Mercado Pago"
                        trimmedLine.contains("scotia", ignoreCase = true) -> dataMap["Banco"] = "Scotiabank"
                        trimmedLine.contains("itau", ignoreCase = true) -> dataMap["Banco"] = "Itaú"
                        trimmedLine.contains("tenpo", ignoreCase = true) -> dataMap["Banco"] = "Tenpo"
                        trimmedLine.contains("tapp", ignoreCase = true) -> dataMap["Banco"] = "TAPP"
                        trimmedLine.contains("copec", ignoreCase = true) -> dataMap["Banco"] = "Copec"
                        trimmedLine.contains("mach", ignoreCase = true) -> dataMap["Banco"] = "MACH"
                    }
                }
                trimmedLine.matches(Regex(".*\\d{1,2}[.]\\d{3}[.]\\d{3}-[\\dkK].*")) -> {
                    dataMap["RUT"] = trimmedLine.replace(Regex("[^0-9Kk-]"), "")
                }
                trimmedLine.contains("@") && trimmedLine.contains(".") -> {
                    dataMap["Correo"] = trimmedLine
                }
                trimmedLine.contains("cuenta", ignoreCase = true) ||
                        trimmedLine.contains("cta", ignoreCase = true) -> {
                    when {
                        trimmedLine.contains("ahorro", ignoreCase = true) -> dataMap["Tipo de Cuenta"] = "Cuenta de Ahorro"
                        trimmedLine.contains("corriente", ignoreCase = true) -> dataMap["Tipo de Cuenta"] = "Cuenta Corriente"
                        trimmedLine.contains("chequera", ignoreCase = true) -> dataMap["Tipo de Cuenta"] = "Chequera Electrónica"
                        trimmedLine.contains("vista", ignoreCase = true) -> dataMap["Tipo de Cuenta"] = "Cuenta Vista"
                    }
                }
                trimmedLine.matches(Regex(".*\\d{7,20}.*")) &&
                        !trimmedLine.contains("rut", ignoreCase = true) &&
                        dataMap["Número de Cuenta"] == "No disponible" -> {
                    dataMap["Número de Cuenta"] = trimmedLine.replace(Regex("[^0-9]"), "")
                }
            }
        }
        return dataMap
    }

    private fun copyToClipboard(data: String) {
        val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Detected Data", data)
        clipboard.setPrimaryClip(clip)
    }

    private fun showCopiedIndicator() {
        Toast.makeText(this, "Copiado al portapapeles", Toast.LENGTH_SHORT).show()
    }

    companion object {
        const val EXTRA_DETECTED_TEXT = "detected_text"
        const val EXTRA_BANK_DATA = "detected_text"
    }
}