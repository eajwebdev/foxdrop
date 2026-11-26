package ph.foxdrop.business.home

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode
import ph.foxdrop.business.start.GetStartedData
import ph.foxdrop.business.start.GetStartedScreen
import ph.foxdrop.core.common.result.DomainError
import ph.foxdrop.core.designsystem.component.Loadable
import ph.foxdrop.core.domain.operator.ApplicationStatus
import ph.foxdrop.core.domain.operator.LicenseStatus
import ph.foxdrop.core.domain.partner.StoreStatus
import ph.foxdrop.core.testing.fake.SampleData
import ph.foxdrop.core.testing.screenshot.FoxScreenshotVariant
import ph.foxdrop.core.testing.screenshot.captureFoxScreenshots

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class BusinessHomeScreenshotTest {
    private val themes = listOf(FoxScreenshotVariant.PhoneLight, FoxScreenshotVariant.PhoneDark)
    private val light = listOf(FoxScreenshotVariant.PhoneLight)

    @Test
    fun home() = captureFoxScreenshots("business_store_home") { StoreHomeScreen(onSignOut = {}) }

    @Test
    fun operatorHome() = captureFoxScreenshots("business_operator_home") {
        OperatorHomeScreen(onSignOut = {}, onOpenZones = {}, onOpenRates = {})
    }

    @Test
    fun operatorAccount() =
        captureFoxScreenshots("business_operator_account", themes) {
            OperatorHomeScreen(onSignOut = {}, onOpenZones = {}, onOpenRates = {}, initialTab = OperatorTab.Account)
        }

    @Test
    fun operatorOrders() =
        captureFoxScreenshots("business_operator_orders", light) {
            OperatorHomeScreen(onSignOut = {}, onOpenZones = {}, onOpenRates = {}, initialTab = OperatorTab.Orders)
        }

    @Test
    fun account() =
        captureFoxScreenshots("business_home_account", themes) {
            StoreHomeScreen(onSignOut = {}, initialTab = 3)
        }

    @Test
    fun getStarted() =
        captureFoxScreenshots("business_get_started") {
            GetStartedScreen(Loadable.Ready(GetStartedData(null, null)), {}, {}, {}, {})
        }

    @Test
    fun getStartedWithApplications() {
        val store = SampleData.myStore(StoreStatus.Rejected, "Permit is expired")
        val application = SampleData.application(
            ApplicationStatus.Approved,
            SampleData.license(LicenseStatus.PendingPayment),
        )
        val data = Loadable.Ready(GetStartedData(store, application))
        captureFoxScreenshots("business_get_started_applied", themes) { GetStartedScreen(data, {}, {}, {}, {}) }
    }

    @Test
    fun getStartedLoadingAndError() {
        captureFoxScreenshots("business_get_started_loading", light) {
            GetStartedScreen(Loadable.Loading, {}, {}, {}, {})
        }
        captureFoxScreenshots("business_get_started_error", light) {
            GetStartedScreen(Loadable.Failed(DomainError.Network), {}, {}, {}, {})
        }
    }
}
