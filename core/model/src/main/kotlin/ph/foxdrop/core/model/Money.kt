package ph.foxdrop.core.model

import kotlinx.serialization.Serializable

/**
 * An amount of Philippine pesos stored as whole centavos (₱1.00 = 100).
 * Money is never a Double/Float (MASTER_PROMPT.md §0 rule 3). Prices and balances shown in the apps
 * always come from the server; this type only carries and displays them.
 */
@Serializable
@JvmInline
value class Money(val centavos: Long) : Comparable<Money> {
    operator fun plus(other: Money): Money = Money(Math.addExact(centavos, other.centavos))

    operator fun minus(other: Money): Money = Money(Math.subtractExact(centavos, other.centavos))

    operator fun times(quantity: Int): Money = Money(Math.multiplyExact(centavos, quantity.toLong()))

    operator fun unaryMinus(): Money = Money(Math.negateExact(centavos))

    override fun compareTo(other: Money): Int = centavos.compareTo(other.centavos)

    val isNegative: Boolean get() = centavos < 0

    companion object {
        const val CENTAVOS_PER_PESO = 100L

        val ZERO = Money(0)

        fun ofPesos(pesos: Long): Money = Money(Math.multiplyExact(pesos, CENTAVOS_PER_PESO))
    }
}
