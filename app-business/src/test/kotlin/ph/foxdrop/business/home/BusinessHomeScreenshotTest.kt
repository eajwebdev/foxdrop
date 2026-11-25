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
    fun home() = captureFoxScreenshots("business_store_home") { StoreHomeScreen(onSignOut = {}) }

    @Test
    fun operatorHome() = captureFoxScreenshots("business_operator_home") { OperatorHomeScreen(onSignOut = {}) }

    @Test
    fun account() =
        captureFoxScreenshots(
            "business_home_account",
            listOf(FoxScreenshotVariant.PhoneLight, FoxScreenshotVariant.PhoneDark),
        ) {
            StoreHomeScreen(onSignOut = {}, initialTab = 3)
        }
}
