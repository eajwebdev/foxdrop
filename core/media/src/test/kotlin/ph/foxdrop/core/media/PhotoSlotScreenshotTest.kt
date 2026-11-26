package ph.foxdrop.core.media

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode
import ph.foxdrop.core.testing.screenshot.FoxScreenshotVariant
import ph.foxdrop.core.testing.screenshot.captureFoxScreenshots

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PhotoSlotScreenshotTest {
    @Test
    fun empty() =
        captureFoxScreenshots(
            "photo_slot_empty",
            listOf(FoxScreenshotVariant.PhoneLight, FoxScreenshotVariant.PhoneDark),
        ) {
            PhotoSlot(label = "Valid ID", photo = null, onTakePhoto = {
            }, onChoosePhoto = {}, errorMessage = "Add a photo")
        }
}
