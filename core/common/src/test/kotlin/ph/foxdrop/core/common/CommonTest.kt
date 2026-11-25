package ph.foxdrop.core.common

import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import ph.foxdrop.core.common.format.formatPeso
import ph.foxdrop.core.common.result.DomainError
import ph.foxdrop.core.common.result.FoxResult
import ph.foxdrop.core.common.result.map
import ph.foxdrop.core.common.result.onFailure
import ph.foxdrop.core.common.result.onSuccess
import ph.foxdrop.core.common.time.ManilaTime
import ph.foxdrop.core.common.validation.EmailAddress
import ph.foxdrop.core.common.validation.Password
import ph.foxdrop.core.common.validation.PhMobileNumber
import ph.foxdrop.core.model.Money

class PesoFormatterTest {
    @Test
    fun `formats centavos with grouping and two decimals`() {
        assertEquals("₱0.00", Money(0).formatPeso())
        assertEquals("₱0.05", Money(5).formatPeso())
        assertEquals("₱40.00", Money(4_000).formatPeso())
        assertEquals("₱218.00", Money(21_800).formatPeso())
        assertEquals("₱1,234.56", Money(123_456).formatPeso())
        assertEquals("₱15,000.00", Money(1_500_000).formatPeso())
        assertEquals("₱1,248,500.00", Money(124_850_000).formatPeso())
    }

    @Test
    fun `negative amounts keep the sign in front`() {
        assertEquals("-₱15.00", Money(-1_500).formatPeso())
        assertEquals("-₱92,233,720,368,547,758.08", Money(Long.MIN_VALUE).formatPeso())
    }
}

class ValidatorsTest {
    @Test
    fun `normalizes PH mobile numbers to E164`() {
        listOf("09171234567", "9171234567", "639171234567", "+63 917 123 4567", "(0917) 123-4567").forEach {
            assertEquals(it, "+639171234567", PhMobileNumber.normalize(it))
        }
    }

    @Test
    fun `rejects non mobile numbers`() {
        listOf("", "0917123456", "08171234567", "+1 917 123 4567", "0917abc4567", "091712345678").forEach {
            assertNull(it, PhMobileNumber.normalize(it))
        }
    }

    @Test
    fun `national digits for a field that already shows +63`() {
        assertEquals("9171234567", PhMobileNumber.nationalDigits("0917 123 4567"))
        assertEquals("9171234567", PhMobileNumber.nationalDigits("+63 917-123-45678"))
        assertEquals("917", PhMobileNumber.nationalDigits("0917"))
        assertEquals("63", PhMobileNumber.nationalDigits("63"))
    }

    @Test
    fun `formats E164 numbers for display`() {
        assertEquals("+63 917 123 4567", PhMobileNumber.format("+639171234567"))
        assertEquals("+6312", PhMobileNumber.format("+6312"))
    }

    @Test
    fun `email and password rules`() {
        assertTrue(EmailAddress.isValid("juan.delacruz@example.ph"))
        assertFalse(EmailAddress.isValid("juan@"))
        assertTrue(Password.isStrongEnough("foxdrop2026"))
        assertFalse(Password.isStrongEnough("short1"))
        assertFalse(Password.isStrongEnough("lettersonly"))
    }
}

class FoxResultTest {
    @Test
    fun `map and callbacks`() {
        var seen = 0
        val ok: FoxResult<Int> = FoxResult.Success(2)
        assertEquals(FoxResult.Success(4), ok.map { it * 2 }.onSuccess { seen = it })
        assertEquals(4, seen)

        var error: DomainError? = null
        val failed: FoxResult<Int> = FoxResult.Failure(DomainError.Network)
        assertEquals(failed, failed.map { it * 2 }.onFailure { error = it })
        assertEquals(DomainError.Network, error)
    }
}

class ManilaTimeTest {
    @Test
    fun `shows Manila time regardless of device zone`() {
        val instant = Instant.parse("2026-11-25T02:24:00Z")
        assertEquals("10:24 AM", ManilaTime.formatTime(instant))
        assertEquals("Nov 25, 2026", ManilaTime.formatDate(instant))
        val now = ManilaTime.now(Clock.fixed(instant, ZoneOffset.UTC))
        assertEquals(10, now.hour)
    }
}
