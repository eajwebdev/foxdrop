package ph.foxdrop.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MoneyTest {
    @Test
    fun `pesos convert to centavos`() {
        assertEquals(1_500_000L, Money.ofPesos(15_000).centavos)
    }

    @Test
    fun `arithmetic stays in whole centavos`() {
        val burger = Money(12_900)
        val fries = Money(4_900)
        val fee = Money(4_000)
        assertEquals(Money(21_800), burger + fries + fee)
        assertEquals(Money(25_800), burger * 2)
        assertEquals(Money(8_000), burger - fries)
        assertEquals(Money(-12_900), -burger)
    }

    @Test
    fun `negative and comparison`() {
        assertTrue(Money(-1).isNegative)
        assertFalse(Money.ZERO.isNegative)
        assertTrue(Money(100) > Money(99))
    }

    @Test(expected = ArithmeticException::class)
    fun `overflow throws instead of wrapping`() {
        Money(Long.MAX_VALUE) + Money(1)
    }

    @Test
    fun `roles map to database labels`() {
        assertEquals(AppRole.StoreOwner, AppRole.fromDbValue("store_owner"))
        assertNull(AppRole.fromDbValue("nope"))
        assertTrue(AppRole.Operator in AppRole.OPERATOR)
        assertFalse(AppRole.SuperAdmin in AppRole.OPERATOR + AppRole.STORE)
    }
}
