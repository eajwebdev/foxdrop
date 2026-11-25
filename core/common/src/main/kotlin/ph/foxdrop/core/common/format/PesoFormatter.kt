package ph.foxdrop.core.common.format

import ph.foxdrop.core.model.Money

/**
 * Formats [Money] as `₱1,234.00`. Locale-independent on purpose: the format is the same on every phone.
 */
object PesoFormatter {
    private const val PESO = "₱"
    private const val GROUP = 3
    private const val MIN_DIGITS = 3 // "0.05" needs at least one peso digit and two centavo digits

    fun format(money: Money): String {
        val centavos = money.centavos
        // Work on the magnitude as an unsigned decimal string so Long.MIN_VALUE is safe.
        val digits = centavos.toString().removePrefix("-").padStart(MIN_DIGITS, '0')
        val pesos = digits.dropLast(2)
        val cents = digits.takeLast(2)
        val grouped = pesos.reversed().chunked(GROUP).joinToString(",").reversed()
        val sign = if (centavos < 0) "-" else ""
        return "$sign$PESO$grouped.$cents"
    }
}

fun Money.formatPeso(): String = PesoFormatter.format(this)
