package ph.foxdrop.core.common.format

/**
 * Peso amounts typed by a person: "40", "40.5", "1,250.00" → centavos. No symbol, no negatives, at most two
 * decimals. Display back with [toInput] ("40.00" → "40", "40.50" → "40.50").
 */
object PesoInput {
    private val pattern = Regex("""^\d{1,9}(\.\d{1,2})?$""")
    private const val CENTAVOS = 100

    fun parse(text: String): Long? {
        val cleaned = text.trim().replace(",", "")
        if (!pattern.matches(cleaned)) return null
        val (pesos, cents) = cleaned.split('.').let { it[0] to it.getOrNull(1).orEmpty() }
        return pesos.toLong() * CENTAVOS + cents.padEnd(2, '0').ifEmpty { "0" }.toLong()
    }

    fun toInput(centavos: Long): String {
        val pesos = centavos / CENTAVOS
        val cents = centavos % CENTAVOS
        return if (cents == 0L) pesos.toString() else "$pesos.${cents.toString().padStart(2, '0')}"
    }
}
