package ph.foxdrop.customer.home

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode
import ph.foxdrop.core.testing.screenshot.FoxScreenshotVariant
import ph.foxdrop.core.testing.screenshot.captureFoxScreenshots

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class CustomerHomeScreenshotTest {
    @Test
    fun home() = captureFoxScreenshots("customer_home") { CustomerHomeScreen(onSignOut = {}) }

    @Test
    fun account() =
        captureFoxScreenshots(
            "customer_home_account",
            listOf(FoxScreenshotVariant.PhoneLight, FoxScreenshotVariant.PhoneDark),
        ) {
            CustomerHomeScreen(onSignOut = {}, initialTab = 2)
        }
}
