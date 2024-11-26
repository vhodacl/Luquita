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
            "Pero... 20 Luquitas, son 20 Luquitas, ¡jajá!",
        ),
        TransferFact(
            "¿Sabías que el primer cajero automático se instaló en Londres en 1967? ¡El PIN original tenía solo 4 dígitos porque la esposa del inventor no podía recordar más!",
            "Barclays Bank"
        ),
        TransferFact(
            "¿Por qué el billete estaba triste? Porque lo dejaron solo en la billetera",
        ),
        TransferFact(
            "¿Qué le dice una moneda a otra moneda? Nos vemos en el cambio",
        ),
        TransferFact(
            "¿Sabías que en la isla de Yap usaban piedras gigantes como moneda? ¡Imagina hacer una transferencia con eso!",
            "Isla de Yap, Micronesia"
        ),
        TransferFact(
            "¿Por qué el Bitcoin fue al psicólogo? Porque tenía muchos altibajos",
        ),
        TransferFact(
            "La primera 'transferencia espacial' se realizó en 2005 cuando un astronauta pagó su factura de agua desde la Estación Espacial Internacional",
            "NASA"
        ),
        TransferFact(
            "¿Qué le dice un préstamo a otro? Préstame atención",
        ),
        TransferFact(
            "En Suecia, solo el 1% de las transacciones se realizan en efectivo. ¡Son tan digitales que hasta los mendigos aceptan pagos con tarjeta!",
            "Banco Central de Suecia"
        ),
        TransferFact(
            "¿Por qué el 5 no le prestó dinero al 7? Porque el 7 se comió al 9",
        ),
        TransferFact(
            "¿Sabías que los antiguos romanos ya hacían transferencias? Usaban notas llamadas 'chirographum', ¡el primer internet banking!",
            "Imperio Romano"
        ),
        TransferFact(
            "Mi billetera es como una cebolla: cuando la abro me dan ganas de llorar",
        ),
        TransferFact(
            "¿Qué hace un banco en la playa? Cuenta olas",
        ),
        TransferFact(
            "En Japón, algunas personas aún guardan sus ahorros bajo el tatami. ¡Los bancos les llaman 'inversores de colchón'!",
            "Banco de Japón"
        ),
        TransferFact(
            "¿Cuál es el colmo de un banco? Que se quede sin cuenta",
        ),
    )

    fun getRandomFact(): TransferFact {
        return facts.random()
    }
} 