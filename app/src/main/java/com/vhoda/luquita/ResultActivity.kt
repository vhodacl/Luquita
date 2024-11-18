package com.vhoda.luquita

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.vhoda.luquita.databinding.ActivityResultBinding
import com.google.android.material.button.MaterialButton

class ResultActivity : AppCompatActivity() {
    private lateinit var binding: ActivityResultBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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

        // Si los datos no son válidos, volvemos a la cámara
        if (!isDataValid(dataMap)) {
            startActivity(Intent(this, CameraActivity::class.java))
            finish()
            return
        }

        // Configurar la UI con los datos parseados
        setupUI(dataMap)
    }

    private fun setupUI(dataMap: Map<String, String>) {
        with(binding) {
            // Configuración de los campos (mapear TextViews y el campo correspondiente en el mapa)
            val fieldMappings = listOf(
                Triple(companyNameTextView, companyNameTextView.parent as View, "Nombre"),
                Triple(rutTextView, rutTextView.parent as View, "RUT"),
                Triple(emailTextView, emailTextView.parent as View, "Correo"),
                Triple(bankTextView, bankTextView.parent as View, "Banco"),
                Triple(accountTypeTextView, accountTypeTextView.parent as View, "Tipo de Cuenta"),
                Triple(accountNumberTextView, accountNumberTextView.parent as View, "Número de Cuenta")
            )

            // Configuración de cada campo
            fieldMappings.forEach { (textView, parentView, key) ->
                setupField(
                    valueTextView = textView,
                    parentView = parentView,
                    value = dataMap[key],
                    key = key
                )
            }

            // Configuración de los botones
            setupButtons(dataMap)
        }
    }

    private fun setupButtons(dataMap: Map<String, String>) {
        with(binding) {
            val hasInvalidFields = dataMap.any { it.value == "No disponible" }

            // Configuración del botón de reintento
            retryButton.apply {
                visibility = if (hasInvalidFields) View.VISIBLE else View.GONE
                setOnClickListener {
                    if (hasInvalidFields) {
                        startActivity(Intent(this@ResultActivity, CameraActivity::class.java))
                        finish()
                    }
                }
            }

            // Configuración del botón de "Hecho"
            doneButton.setOnClickListener { finish() }

            // Configuración del botón Copiar Todo
            copyAllCard.setOnClickListener {
                val allData = dataMap
                    .map { it.value ?: "" }  // Convertimos los valores nulos a cadenas vacías
                    .joinToString(separator = "\n")  // Unimos los valores con salto de línea

                // Copiar al portapapeles
                copyToClipboard(allData)
                showCopiedIndicator()  // Mostrar mensaje de "Copiado"
            }
        }
    }

    private fun setupField(
        valueTextView: TextView,
        parentView: View,
        value: String?,
        key: String
    ) {
        // Si el valor está vacío o es nulo, ponemos "No disponible"
        val finalValue = value?.takeIf { it.isNotBlank() } ?: "No disponible"

        // Configuramos el TextView con el valor
        valueTextView.apply {
            text = finalValue
            contentDescription = "$key: $finalValue"
        }

        // Configuramos el ImageView para indicar si el campo es válido o no
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

    // Método para parsear la cadena de datos del banco
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
                "Nombre" to "Nombre No disponible",
                "RUT" to "Rut No disponible",
                "Banco" to "Banco No disponible",
                "Tipo de Cuenta" to "T. Cuenta No disponible",
                "Número de Cuenta" to "N. Cuenta No disponible",
                "Correo" to "Correo No disponible"
            )
        }
    }

    // Verificar si los datos son válidos (al menos 3 campos no disponibles)
    private fun isDataValid(dataMap: Map<String, String>): Boolean {
        var validFields = 0
        dataMap.forEach { (_, value) ->
            if (value != "No disponible" && value.isNotBlank()) validFields++
        }
        return validFields >= 3
    }

    // Parsear el texto detectado por OCR
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
                        trimmedLine.contains("bci", ignoreCase = true) -> dataMap["Banco"] = "BCI"
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
                        trimmedLine.contains("rut", ignoreCase = true) -> dataMap["Tipo de Cuenta"] = "Cuenta RUT"
                        trimmedLine.contains("corriente", ignoreCase = true) -> dataMap["Tipo de Cuenta"] = "Cuenta Corriente"
                        trimmedLine.contains("vista", ignoreCase = true) -> dataMap["Tipo de Cuenta"] = "Cuenta Vista"
                        trimmedLine.contains("ahorro", ignoreCase = true) -> dataMap["Tipo de Cuenta"] = "Cuenta de Ahorro"
                    }
                }
                trimmedLine.matches(Regex(".*\\d{7,20}.*")) &&
                        !trimmedLine.contains("rut", ignoreCase = true) &&
                        dataMap["Número de Cuenta"] == "No disponible" -> {
                    dataMap["Número de Cuenta"] = trimmedLine.replace(Regex("[^0-9]"), "")
                }
                (trimmedLine.contains("digito") || trimmedLine.contains("verificador")) &&
                        dataMap["RUT"] == "No disponible" -> {
                    dataMap["RUT"] = trimmedLine.split(":").lastOrNull()?.trim() ?: ""
                }
            }
        }
        return dataMap
    }

    // Extraer el valor después de ":", por ejemplo en "Banco: Santander"
    private fun extractValue(line: String): String {
        return line.substringAfter(":").trim()
    }

    // Copiar al portapapeles
    private fun copyToClipboard(data: String) {
        val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Detected Data", data)
        clipboard.setPrimaryClip(clip)
    }

    // Mostrar mensaje de copiado
    private fun showCopiedIndicator() {
        Toast.makeText(this, "Copiado al portapapeles", Toast.LENGTH_SHORT).show()
    }

    companion object {
        const val EXTRA_DETECTED_TEXT = "detected_text"
    }
}
