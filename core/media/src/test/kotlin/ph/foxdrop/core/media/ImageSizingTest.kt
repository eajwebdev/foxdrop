package ph.foxdrop.core.media

import org.junit.Assert.assertEquals
import org.junit.Test

class ImageSizingTest {
    @Test
    fun `small photos keep their size`() {
        assertEquals(1200 to 900, targetSize(1200, 900, 1600))
        assertEquals(1, sampleSize(1200, 900, 1600))
    }

    @Test
    fun `big photos shrink to 1600 on the long edge, keeping the shape`() {
        assertEquals(1600 to 1200, targetSize(4000, 3000, 1600))
        assertEquals(900 to 1600, targetSize(2700, 4800, 1600))
    }

    @Test
    fun `the first decode skips by powers of two without going below the target`() {
        assertEquals(2, sampleSize(4000, 3000, 1600))
        assertEquals(4, sampleSize(8000, 6000, 1600))
        assertEquals(1, sampleSize(3000, 2000, 1600))
    }

    @Test
    fun `thumbnails decode with a step that keeps the short edge at the thumbnail size`() {
        assertEquals(8, powerOfTwoStep(1200, 128))
        assertEquals(1, powerOfTwoStep(100, 128))
        assertEquals(4, powerOfTwoStep(900, 168))
    }
}
