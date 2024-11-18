// BankDataParser.kt
package com.vhoda.luquita  // Importante: mismo paquete que BankData

object BankDataParser {
    // Lista de palabras clave de bancos conocidos
    private val knownBanks = mapOf(
        "estado" to "Banco Estado",
        "cuenta rut" to "Banco Estado",
        "cta rut" to "Banco Estado",
        "ctarut" to "Banco Estado",
        "cta.rut" to "Banco Estado",
        "bci" to "Banco Crédito e Inversiones",
        "banco chile" to "Banco de Chile",
        "banco de chile" to "Banco de Chile",
        "Santander" to "Banco Santander",
        "santander" to "Banco Santander",
        "mercado pago" to "Mercado Pago",
        "ripley" to "Banco Ripley",
        "falabella" to "Banco Falabella",
        "scotiabank" to "Scotiabank",
        "itau" to "Itaú",
        "tenpo" to "Tenpo",
        "tapp" to "TAPP",
        "copec" to "Copec",
        "mach" to "MACH"
    )

    fun parse(text: String): com.vhoda.luquita.BankData {  // Referencia completa a la clase BankData
        val bankData = com.vhoda.luquita.BankData()  // Crear instancia con referencia completa
        val lines = text.split("\n")

        for (line in lines) {
            val normalizedLine = line.trim().lowercase()

            when {
                // Detectar bancos basados en palabras clave conocidas
                knownBanks.keys.any { normalizedLine.contains(it) } -> {
                    val matchedBank = knownBanks.entries.find { normalizedLine.contains(it.key) }
                    if (matchedBank != null) {
                        bankData.bank = matchedBank.value
                    }
                }

                // Detectar tipos de cuenta basados en palabras clave
                normalizedLine.contains("cuenta") || normalizedLine.contains("cta") -> {
                    when {
                        normalizedLine.contains("rut") -> bankData.accountType = "Cuenta RUT"
                        normalizedLine.contains("vista") -> bankData.accountType = "Cuenta Vista"
                        normalizedLine.contains("corriente") -> bankData.accountType = "Cuenta Corriente"
                        normalizedLine.contains("eletronica") -> bankData.accountType = "Chequera Electrónica"
                        normalizedLine.contains("chequera") -> bankData.accountType = "Chequera Electrónica"
                        normalizedLine.contains("ahorro") -> bankData.accountType = "Cuenta de Ahorro"
                    }
                }

                // Detectar número de cuenta (7-20 dígitos)
                line.matches(Regex(".*\\d{7,20}.*")) &&
                        !line.contains("rut", true) &&
                        bankData.accountNumber == null -> {
                    bankData.accountNumber = line.replace(Regex("[^0-9]"), "")
                }

                // Detectar RUT (formato xx.xxx.xxx-x o xxxxxxxx-x)
                line.matches(Regex(".*\\b\\d{1,2}\\.?\\d{3}\\.?\\d{3}-\\d\\b.*")) -> {
                    bankData.rut = line.replace(Regex("[^\\dxX.-]"), "").trim()
                }

                // Detectar correos electrónicos
                line.contains("@") && line.matches(Regex("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}")) -> {
                    bankData.email = line.trim()
                }

                // Detectar nombres de empresa o persona
                (line.contains("ltda", true) ||
                        line.contains("s.a", true) ||
                        line.contains("spa", true) ||
                        (line.length > 5 && !line.contains("@") &&
                                !line.matches(Regex(".*\\d{7,20}.*")))) -> {
                    if (bankData.companyName == null) {
                        bankData.companyName = line.trim()
                    }
                }
            }
        }

        return bankData
    }
}