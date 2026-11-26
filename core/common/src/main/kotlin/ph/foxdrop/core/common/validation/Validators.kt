package ph.foxdrop.core.common.validation

/**
 * Philippine mobile numbers. Accepts `09171234567`, `9171234567`, `639171234567`, `+63 917 123 4567`
 * and returns E.164 (`+639171234567`), or null when it is not a PH mobile number.
 * The server validates again; this only gives fast feedback.
 */
object PhMobileNumber {
    const val NATIONAL_LENGTH = 10
    private val separators = Regex("[\\s()-]")
    private val e164Groups = Regex("""\+63(\d{3})(\d{3})(\d{4})""")

    fun normalize(input: String): String? {
        val digits = input.replace(separators, "").removePrefix("+")
        if (!digits.all(Char::isDigit)) return null
        val national = stripPrefix(digits)
        return if (national.length == NATIONAL_LENGTH && national.startsWith("9")) "+63$national" else null
    }

    /**
     * For a field that already shows "+63": people type "0917…" or "+63 917…", so keep only the 10 national digits.
     */
    fun nationalDigits(input: String): String = stripPrefix(input.filter(Char::isDigit)).take(NATIONAL_LENGTH)

    /** `+639171234567` → `+63 917 123 4567`; anything else is returned unchanged. */
    fun format(e164: String): String =
        e164Groups.matchEntire(e164)?.destructured?.let { (a, b, c) -> "+63 $a $b $c" } ?: e164

    private fun stripPrefix(digits: String): String =
        when {
            digits.startsWith("63") && digits.length > NATIONAL_LENGTH -> digits.drop(2)
            digits.startsWith("0") -> digits.drop(1)
            else -> digits
        }
}

object EmailAddress {
    private val pattern = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

    fun isValid(input: String): Boolean = pattern.matches(input.trim())
}

object Password {
    const val MIN_LENGTH = 8

    /** At least 8 characters with a letter and a digit. The server checks the same rule. */
    fun isStrongEnough(input: String): Boolean =
        input.length >= MIN_LENGTH && input.any(Char::isLetter) && input.any(Char::isDigit)
}
