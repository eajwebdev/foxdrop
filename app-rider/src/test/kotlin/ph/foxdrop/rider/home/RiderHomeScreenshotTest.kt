package ph.foxdrop.rider.home

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode
import ph.foxdrop.core.testing.screenshot.FoxScreenshotVariant
import ph.foxdrop.core.testing.screenshot.captureFoxScreenshots

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class RiderHomeScreenshotTest {
    @Test
    fun home() = captureFoxScreenshots("rider_home") { RiderHomeScreen(onSignOut = {}) }

    @Test
    fun account() =
        captureFoxScreenshots(
            "rider_home_account",
            listOf(FoxScreenshotVariant.PhoneLight, FoxScreenshotVariant.PhoneDark),
        ) {
            RiderHomeScreen(onSignOut = {}, initialTab = 2)
        }
}
