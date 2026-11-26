package ph.foxdrop.core.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import ph.foxdrop.core.common.format.PesoInput

class PesoInputTest {
    @Test
    fun `parses what people type`() {
        assertEquals(4_000L, PesoInput.parse("40"))
        assertEquals(4_050L, PesoInput.parse("40.5"))
        assertEquals(4_005L, PesoInput.parse("40.05"))
        assertEquals(125_000L, PesoInput.parse(" 1,250.00 "))
        assertEquals(0L, PesoInput.parse("0"))
    }

    @Test
    fun `rejects anything that is not a plain amount`() {
        listOf("", "abc", "-5", "1.234", "₱40", "1e3").forEach { assertNull(it, PesoInput.parse(it)) }
    }

    @Test
    fun `shows amounts without needless decimals`() {
        assertEquals("40", PesoInput.toInput(4_000))
        assertEquals("40.50", PesoInput.toInput(4_050))
        assertEquals("0.05", PesoInput.toInput(5))
    }
}
