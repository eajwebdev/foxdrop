package ph.foxdrop.business.home

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode
import ph.foxdrop.core.testing.screenshot.FoxScreenshotVariant
import ph.foxdrop.core.testing.screenshot.captureFoxScreenshots

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class BusinessHomeScreenshotTest {
    @Test
    fun home() = captureFoxScreenshots("business_home") { BusinessHomeScreen(onSignOut = {}) }

    @Test
    fun account() =
        captureFoxScreenshots(
            "business_home_account",
            listOf(FoxScreenshotVariant.PhoneLight, FoxScreenshotVariant.PhoneDark),
        ) {
            BusinessHomeScreen(onSignOut = {}, initialTab = 3)
        }
}
