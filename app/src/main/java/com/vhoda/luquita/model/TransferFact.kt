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
        TransferFact(
            "¿Sabías que en la antigua China usaban cuchillos como moneda? ¡Imagina pagar tu café con uno!",
            "Antigua China"
        ),
        TransferFact(
            "En 2010, alguien compró dos pizzas por 10,000 Bitcoins. ¡Hoy eso valdría más de 300 millones de dólares!",
            "Bitcoin Pizza Day"
        ),
        TransferFact(
            "¿Por qué el euro fue al gimnasio? ¡Para fortalecer su valor!",
        ),
        TransferFact(
            "¿Sabías que M-PESA en Kenia mueve más del 50% del PIB del país a través de pagos móviles?",
            "Banco Central de Kenia"
        ),
        TransferFact(
            "¿Qué le dice un cajero automático a otro? ¡Qué retirado te veo!",
        ),
        TransferFact(
            "En Dinamarca es ilegal pagar en efectivo compras mayores a 50,000 coronas. ¡Todo debe ser digital!",
            "Gobierno de Dinamarca"
        ),
        TransferFact(
            "¿Por qué el dólar no puede dormir? ¡Porque tiene muchas cuentas pendientes!",
        ),
        TransferFact(
            "Los vikingos usaban pulseras de plata como moneda. ¡Literalmente llevaban su dinero puesto!",
            "Historia Vikinga"
        ),
        TransferFact(
            "¿Cuál es el animal más rico? ¡El cerdo, porque siempre tiene plata en el banco!",
        ),
        TransferFact(
            "El primer pago con tarjeta de crédito fue en 1950 en un restaurante de Nueva York. ¡El dueño de la tarjeta se había olvidado su billetera!",
            "Diners Club"
        ),
        TransferFact(
            "¿Sabías que la CuentaRUT fue creada en 2006 y hoy más del 80% de los chilenos tiene una?",
            "BancoEstado"
        ),
        TransferFact(
            "¿Por qué las lucas fueron al Cajero? ¡Porque querían salir a carretear!",
        ),
        TransferFact(
            "Chile fue pionero en Latinoamérica con las transferencias electrónicas. ¡La primera TEF interbancaria se realizó en 2001!",
            "Banco Central de Chile"
        ),
        TransferFact(
            "¿Qué le dice una luca a otra luca? ¡Nos vemos en el Super8!",
        ),
        TransferFact(
            "En Chile, las transferencias electrónicas crecieron un 330% durante la pandemia. ¡De la presencial a la digital al toque!",
            "CMF Chile"
        ),
        TransferFact(
            "¿Por qué la CuentaRUT fue al psicólogo? ¡Porque tenía problemas de autoestima con sus comisiones!",
        ),
        TransferFact(
            "¿Sabías que en Chile se realizan más de 100 millones de transferencias electrónicas al mes? ¡Somos más digitales que el mote con huesillo!",
            "Banco Central de Chile"
        ),
        TransferFact(
            "¿Qué le dice un peso chileno a un dólar? 'Algún día te alcanzaré... pero no hoy día'",
        ),
        TransferFact(
            "La primera tarjeta de débito en Chile fue emitida en 1987. ¡Y ahora hasta pagamos con el celular!",
            "Transbank"
        ),
        TransferFact(
            "¿Sabías que los chilenos prefieren transferir que pagar en efectivo? ¡El 76% de las transacciones son digitales!",
            "CMF Chile"
        )
    )

    fun getRandomFact(): TransferFact {
        return facts.random()
    }
} 