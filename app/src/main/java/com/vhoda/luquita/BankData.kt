import java.io.Serializable

data class BankData(
    var rut: String? = null,
    var email: String? = null,
    var bank: String? = null,
    var accountType: String? = null,
    var accountNumber: String? = null,
    var companyName: String? = null
) : Serializable {  // Añadir Serializable

    fun isValid(): Boolean {
        var validFields = 0
        if (!rut.isNullOrBlank() && isValidRut(rut!!)) validFields++
        if (!accountNumber.isNullOrBlank()) validFields++
        if (!bank.isNullOrBlank()) validFields++
        if (!accountType.isNullOrBlank()) validFields++
        if (!email.isNullOrBlank() && isValidEmail(email!!)) validFields++
        return validFields >= 3
    }

    private fun isValidRut(rut: String): Boolean {
        return rut.matches(Regex("\\d{1,2}\\.?\\d{3}\\.?\\d{3}-[\\dkK]"))
    }

    private fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    fun toMap(): Map<String, String> = mapOf(
        "RUT" to (rut ?: "No disponible"),
        "Correo" to (email ?: "No disponible"),
        "Banco" to (bank ?: "No disponible"),
        "Tipo de Cuenta" to (accountType ?: "No disponible"),
        "Número de Cuenta" to (accountNumber ?: "No disponible"),
        "Nombre" to (companyName ?: "No disponible")
    )

    override fun toString(): String {
        return toMap().entries.joinToString(", ") { "${it.key}=${it.value}" }
    }
}
