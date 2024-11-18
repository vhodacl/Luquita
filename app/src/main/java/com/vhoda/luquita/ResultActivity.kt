package com.vhoda.luquita

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.vhoda.luquita.databinding.ActivityResultBinding
import android.graphics.Color

class ResultActivity : AppCompatActivity() {
    private lateinit var binding: ActivityResultBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResultBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val detectedText = intent.getStringExtra(EXTRA_DETECTED_TEXT) ?: ""
        val bankDataString = intent.getStringExtra("BANK_DATA") ?: ""

        val dataMap = if (bankDataString.isNotBlank()) {
            parseBankDataString(bankDataString)
        } else {
            parseDetectedData(detectedText)
        }

        if (!isDataValid(dataMap)) {
            startActivity(Intent(this, CameraActivity::class.java))
            finish()
            return
        }

        setupUI(dataMap)
    }

    private fun setupUI(dataMap: Map<String, String>) {
        with(binding) {
            // Mapear los campos con sus respectivos TextViews y status ImageViews
            val fieldMappings = listOf(
                Triple(companyNameTextView, companyNameTextView.parent as View, "Nombre"),
                Triple(rutTextView, rutTextView.parent as View, "RUT"),
                Triple(emailTextView, emailTextView.parent as View, "Correo"),
                Triple(bankTextView, bankTextView.parent as View, "Banco"),
                Triple(accountTypeTextView, accountTypeTextView.parent as View, "Tipo de Cuenta"),
                Triple(accountNumberTextView, accountNumberTextView.parent as View, "Número de Cuenta")
            )

            // Configurar cada campo
            fieldMappings.forEach { (textView, parentView, key) ->
                setupField(
                    valueTextView = textView,
                    parentView = parentView,
                    value = dataMap[key],
                    key = key
                )
            }

            // Configurar la capacidad de copiar al portapapeles
            setupCopyListeners(dataMap)
            // Gestionar visibilidad y comportamiento de los botones
            setupButtons(dataMap)
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
            doneButton.setOnClickListener { finish() }
        }
    }

    private fun setupField(
        valueTextView: TextView,
        parentView: View,
        value: String?,
        key: String
    ) {
        val finalValue = value?.takeIf { it.isNotBlank() } ?: "No disponible"

        // Configurar el TextView del valor
        valueTextView.apply {
            text = finalValue
            contentDescription = "$key: $finalValue"
        }

        // Encontrar el ImageView dentro del LinearLayout padre
        val statusImageView = (parentView as? android.widget.LinearLayout)?.let { layout ->
            layout.findViewById<ImageView>(android.R.id.empty)?.apply {
                val isValid = finalValue != "No disponible"
                setImageResource(if (isValid) R.drawable.task_alt else R.drawable.error)
                imageTintList = ContextCompat.getColorStateList(
                    context,
                    if (isValid) R.color.green else R.color.red
                )
                contentDescription = if (isValid) "Campo válido" else "Campo no disponible"
            }
        }
    }

    private fun setupCopyListeners(dataMap: Map<String, String>) {
        val fields = mapOf(
            binding.companyNameTextView to "Nombre",
            binding.rutTextView to "RUT",
            binding.emailTextView to "Correo",
            binding.bankTextView to "Banco",
            binding.accountTypeTextView to "Tipo de Cuenta",
            binding.accountNumberTextView to "Número de Cuenta"
        )

        fields.forEach { (textView, key) ->
            textView.setOnClickListener {
                val value = dataMap[key]
                if (value != null && value != "No disponible") {
                    copyToClipboard(value)
                    showCopiedIndicator()
                }
            }
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
                        trimmedLine.contains("estado", ignoreCase = true) ->
                            dataMap["Banco"] = "Banco Estado"
                        trimmedLine.contains("santander", ignoreCase = true) ->
                            dataMap["Banco"] = "Banco Santander"
                        trimmedLine.contains("chile", ignoreCase = true) ->
                            dataMap["Banco"] = "Banco de Chile"
                        trimmedLine.contains("bci", ignoreCase = true) ->
                            dataMap["Banco"] = "BCI"
                        else -> dataMap["Banco"] = extractValue(trimmedLine)
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
                        trimmedLine.contains("rut", ignoreCase = true) ->
                            dataMap["Tipo de Cuenta"] = "Cuenta RUT"
                        trimmedLine.contains("corriente", ignoreCase = true) ->
                            dataMap["Tipo de Cuenta"] = "Cuenta Corriente"
                        trimmedLine.contains("vista", ignoreCase = true) ->
                            dataMap["Tipo de Cuenta"] = "Cuenta Vista"
                        trimmedLine.contains("ahorro", ignoreCase = true) ->
                            dataMap["Tipo de Cuenta"] = "Cuenta de Ahorro"
                    }
                }
                trimmedLine.matches(Regex(".*\\d{7,20}.*")) &&
                        !trimmedLine.contains("rut", ignoreCase = true) &&
                        dataMap["Número de Cuenta"] == "No disponible" -> {
                    dataMap["Número de Cuenta"] = trimmedLine.replace(Regex("[^0-9]"), "")
                }
                (trimmedLine.contains("ltda", ignoreCase = true) ||
                        trimmedLine.contains("s.a", ignoreCase = true) ||
                        trimmedLine.contains("spa", ignoreCase = true) ||
                        (trimmedLine.length > 5 && !trimmedLine.contains("@") &&
                                !trimmedLine.matches(Regex(".*\\d{7,20}.*")))) -> {
                    if (dataMap["Nombre"] == "No disponible") {
                        dataMap["Nombre"] = trimmedLine
                    }
                }
            }
        }

        return dataMap
    }

    private fun extractValue(line: String): String {
        return line.substringAfter(":", "").takeIf { it.isNotBlank() }
            ?: line.substringAfter("=", "").takeIf { it.isNotBlank() }
            ?: line.trim()
    }

    private fun copyToClipboard(text: String) {
        val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Dato copiado", text)
        clipboard.setPrimaryClip(clip)
    }

    private fun showCopiedIndicator() {
        binding.copiedText.apply {
            visibility = View.VISIBLE
            postDelayed({ visibility = View.GONE }, 2000)
        }
    }

    companion object {
        const val EXTRA_DETECTED_TEXT = "com.vhoda.luquita.EXTRA_DETECTED_TEXT"
    }
}