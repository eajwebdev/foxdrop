package ph.foxdrop.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class GeoPointTest {
    @Test
    fun `positions on the globe are accepted`() {
        assertEquals(10.674, GeoPoint(10.674, 122.953).lat, 0.0)
        GeoPoint(-90.0, 180.0)
    }

    @Test
    fun `positions off the globe are rejected`() {
        assertThrows(IllegalArgumentException::class.java) { GeoPoint(91.0, 0.0) }
        assertThrows(IllegalArgumentException::class.java) { GeoPoint(0.0, -180.5) }
    }
}
