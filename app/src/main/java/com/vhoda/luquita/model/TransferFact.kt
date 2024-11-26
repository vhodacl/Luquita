package com.vhoda.luquita.model

data class TransferFact(
    val fact: String,
    val source: String? = null
)

object TransferFactsProvider {
    private val facts = listOf(
        TransferFact(
            "¿Sabías que la primera transferencia electrónica de dinero se realizó en 1871 a través del telégrafo?",
            "Western Union"
        ),
        TransferFact(
            "El código SWIFT fue creado en 1973 para estandarizar las transferencias internacionales. ¡Antes era un caos!",
        ),
        TransferFact(
            "¿Por qué el dinero fue al médico? ¡Porque necesitaba un cambio!",
        ),
        TransferFact(
            "¿Qué le dijo un peso a otro peso? Nada, porque el dinero no habla.",
        ),
        TransferFact(
            "20 Luquitas, son 20 Luquitas, ¡jajá!",
        ),
        // Agrega más datos curiosos aquí
    )

    fun getRandomFact(): TransferFact {
        return facts.random()
    }
} 