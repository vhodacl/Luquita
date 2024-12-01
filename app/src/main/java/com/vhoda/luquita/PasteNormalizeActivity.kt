package com.vhoda.luquita

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.vhoda.luquita.databinding.ActivityPasteNormalizeBinding
import android.os.Build
import android.content.ClipboardManager
import android.content.Context

class PasteNormalizeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPasteNormalizeBinding
    private var detectedText: String = ""

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
        "Banco BCI/MACH" to listOf("mach")
    )

    private val commonPhrases = listOf(
        "los datos para la transferencia son",
        "los datos son",
        "datos bancarios",
        "datos de transferencia",
        "datos para transferir",
        "datos de la cuenta",
        "los datos de",
        "transferir a",
        "cuenta de",
        "datos de",
        "amig@s",
        "amigos",
        "hola",
        "todo suma",
        "gracias"
    )

    companion object {
        private const val TAG = "PasteNormalizeActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPasteNormalizeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupTransparentBars()
        handleIncomingText()
        
        binding.btnFinalizar.setOnClickListener { finishAndSendResults() }
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

    private fun handleIncomingText() {
        intent.getStringExtra("clipboard_text")?.let { text ->
            binding.progressBar.visibility = View.VISIBLE
            binding.tvOriginalText.text = text

            // Usar el mismo parser que CheckInImageActivity
            val bankData = parseBankData(text)
            detectedText = buildFormattedText(bankData)

            binding.tvDetectedText.text = detectedText
            binding.progressBar.visibility = View.GONE
        } ?: run {
            Toast.makeText(this, "No se recibió texto", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun parseBankData(text: String): BankData {
        val bankData = BankData()
        val lines = text.split("\n").map { it.trim().lowercase() }

        // Regex para detectar correos electrónicos
        val emailRegex = Regex("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}")

        lines.forEach { line ->
            when {
                // Agregar detección de correo electrónico
                line.contains("@") && emailRegex.find(line) != null -> {
                    bankData.email = emailRegex.find(line)?.value
                }

                // Detectar RUT (prioridad alta ya que tiene formato específico)
                line.matches(Regex(".*\\b\\d{1,2}[.]?\\d{3}[.]?\\d{3}-?[\\dkK]\\b.*")) -> {
                    val rutMatch = line.replace(Regex("[^\\dkK.-]"), "").trim()
                    bankData.rut = formatRut(rutMatch)
                    
                    // Si es Banco Estado y Cuenta RUT, usar el RUT como número de cuenta
                    if (bankData.bank == "Banco Estado" && bankData.accountType == "Cuenta RUT") {
                        bankData.accountNumber = rutMatch.replace(Regex("[^0-9]"), "").dropLast(1)
                    }
                }

                // Detectar tipo de cuenta
                line.contains("cuenta") || line.contains("cta") -> {
                    when {
                        line.contains("rut") -> {
                            bankData.accountType = "Cuenta RUT"
                            bankData.bank = "Banco Estado"
                            // Si ya tenemos el RUT, usarlo como número de cuenta
                            if (bankData.rut != null) {
                                bankData.accountNumber = bankData.rut!!.replace(Regex("[^0-9]"), "").dropLast(1)
                            }
                        }
                        line.contains("vista") -> bankData.accountType = "Cuenta Vista"
                        line.contains("corriente") -> bankData.accountType = "Cuenta Corriente"
                        line.contains("electronica") || line.contains("chequera") -> 
                            bankData.accountType = "Chequera Electrónica"
                        line.contains("ahorro") -> bankData.accountType = "Cuenta de Ahorro"
                    }
                }

                // Detectar número de cuenta (números largos sin guiones)
                line.matches(Regex(".*\\b\\d{7,20}\\b.*")) -> {
                    // Solo usar el RUT como número de cuenta para Banco Estado y Cuenta RUT
                    if (bankData.bank == "Banco Estado" && bankData.accountType == "Cuenta RUT") {
                        if (bankData.rut != null) {
                            bankData.accountNumber = bankData.rut!!.replace(Regex("[^0-9]"), "").dropLast(1)
                        }
                    } else {
                        // Para todos los demás casos, usar el número detectado
                        bankData.accountNumber = line.replace(Regex("[^0-9]"), "")
                    }
                }

                // Detectar banco usando keywords
                bankKeywords.any { (_, keywords) -> 
                    keywords.any { keyword -> line.contains(keyword) }
                } -> {
                    bankData.bank = bankKeywords.entries.first { (_, keywords) ->
                        keywords.any { keyword -> line.contains(keyword) }
                    }.key
                }

                // Modificar la detección del nombre
                line.length > 2 && 
                !line.contains(Regex("[0-9@]")) && 
                !line.contains("cuenta", true) && 
                !line.contains("banco", true) &&
                !commonPhrases.any { phrase -> line.lowercase().contains(phrase.lowercase()) } &&
                bankData.companyName.isNullOrBlank() -> {
                    // Limpiar emojis y caracteres especiales
                    val cleanName = line.trim()
                        .replace(Regex("[^\\p{L}\\p{M}\\s]"), "") // Solo letras y espacios
                        .replace(Regex("\\s+"), " ") // Eliminar espacios múltiples
                        .trim()
                    
                    if (cleanName.length > 2) { // Asegurarse de que quede algo después de limpiar
                        bankData.companyName = cleanName.split(" ").joinToString(" ") { 
                            it.capitalize() 
                        }
                    }
                }
            }
        }

        return bankData
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

    private fun String.capitalize(): String {
        return this.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
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
            putExtra("FROM_PASTE", true)
        }
        startActivity(intent)
        finish()
    }
} 