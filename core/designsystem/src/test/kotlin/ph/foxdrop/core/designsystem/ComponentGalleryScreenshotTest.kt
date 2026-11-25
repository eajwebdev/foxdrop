package ph.foxdrop.core.designsystem

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode
import ph.foxdrop.core.designsystem.preview.ComponentGallery
import ph.foxdrop.core.testing.screenshot.FoxScreenshotVariant
import ph.foxdrop.core.testing.screenshot.captureFoxScreenshots

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ComponentGalleryScreenshotTest {
    @Test
    fun componentGallery() =
        captureFoxScreenshots(
            name = "component_gallery",
            variants = listOf(FoxScreenshotVariant.PhoneLight, FoxScreenshotVariant.PhoneDark),
        ) {
            ComponentGallery()
        }
}
