@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package ph.foxdrop.feature.operatorlicense

import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode
import ph.foxdrop.core.common.result.DomainError
import ph.foxdrop.core.designsystem.component.Loadable
import ph.foxdrop.core.domain.operator.ApplicationStatus
import ph.foxdrop.core.domain.operator.LicenseStatus
import ph.foxdrop.core.domain.operator.PaymentStatus
import ph.foxdrop.core.testing.MainDispatcherRule
import ph.foxdrop.core.testing.fake.FakeOperatorLicenseRepository
import ph.foxdrop.core.testing.fake.SampleData
import ph.foxdrop.core.testing.screenshot.FoxScreenshotVariant
import ph.foxdrop.core.testing.screenshot.captureFoxScreenshots

@RunWith(AndroidJUnit4::class)
class LicenseCardViewModelTest {
    @get:Rule
    val main = MainDispatcherRule(StandardTestDispatcher())

    private val licenses = FakeOperatorLicenseRepository()

    @Test
    fun `renewing pays and shows the extended license`() = runTest(main.dispatcher) {
        licenses.license = SampleData.license(LicenseStatus.Grace, -3)
        val vm = LicenseCardViewModel(licenses, PaymentPoller(licenses))
        runCurrent()
        assertEquals(Loadable.Ready(SampleData.license(LicenseStatus.Grace, -3)), vm.state.value.license)

        vm.checkout.test {
            vm.renew()
            runCurrent()
            assertEquals("http://10.0.2.2:8000/dev/checkout/pay-1", awaitItem())
        }
        licenses.license = SampleData.license(LicenseStatus.Active, 362)
        advanceTimeBy(2_001)
        runCurrent()

        assertEquals(PayState.Idle, vm.state.value.pay)
        assertEquals(Loadable.Ready(SampleData.license(LicenseStatus.Active, 362)), vm.state.value.license)
    }

    @Test
    fun `no license and errors are shown, and a lost connection while polling is reported`() = runTest(
        main.dispatcher,
    ) {
        licenses.loadError = DomainError.Network
        val vm = LicenseCardViewModel(licenses, PaymentPoller(licenses))
        runCurrent()
        assertEquals(Loadable.Failed(DomainError.Network), vm.state.value.license)

        licenses.loadError = null
        vm.load()
        runCurrent()
        assertEquals(Loadable.Ready(null), vm.state.value.license)
        vm.renew() // nothing to renew
        assertEquals(PayState.Idle, vm.state.value.pay)
    }
}

@RunWith(AndroidJUnit4::class)
class PaymentPollerTest {
    @get:Rule
    val main = MainDispatcherRule(StandardTestDispatcher())

    @Test
    fun `polls quickly at first, then backs off, for about two minutes`() {
        val delays = PaymentPoller.POLL_DELAYS_MS
        assertEquals(listOf(2_000L, 2_000L, 3_000L, 5_000L, 8_000L), delays.take(5))
        assertEquals(116_000L, delays.sum())
    }

    @Test
    fun `network blips are retried, other errors stop at once`() = runTest(main.dispatcher) {
        val licenses =
            object : ph.foxdrop.core.domain.operator.OperatorLicenseRepository by FakeOperatorLicenseRepository() {
                var answers =
                    mutableListOf<ph.foxdrop.core.common.result.FoxResult<ph.foxdrop.core.domain.operator.Payment>>(
                        ph.foxdrop.core.common.result.FoxResult.Failure(DomainError.Network),
                        ph.foxdrop.core.common.result.FoxResult.Success(SampleData.payment(PaymentStatus.Expired)),
                    )

                override suspend fun payment(paymentId: String) = answers.removeAt(0)
            }
        assertEquals(PaymentOutcome.NotPaid, PaymentPoller(licenses).await("pay-1"))

        licenses.answers = mutableListOf(ph.foxdrop.core.common.result.FoxResult.Failure(DomainError.Unauthorized))
        assertEquals(PaymentOutcome.Error(DomainError.Unauthorized), PaymentPoller(licenses).await("pay-1"))
    }
}

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class OperatorLicenseScreenshotTest {
    private val twoThemes = listOf(FoxScreenshotVariant.PhoneLight, FoxScreenshotVariant.PhoneDark)

    private fun screen(name: String, state: OperatorLicenseState, variants: List<FoxScreenshotVariant> = twoThemes) =
        captureFoxScreenshots(name, variants) {
            OperatorLicenseScreen(state = state, onAction = {}, onBack = {}, onTakePhoto = {}, onChoosePhoto = {})
        }

    @Test
    fun apply() =
        screen(
            "operator_license_apply",
            OperatorLicenseState.Apply(
                ApplyForm(
                    provinces = LicensePreviewData.provinces,
                    provinceId = "ilo",
                    businessName = "Gonzaga Logistics",
                ),
            ),
            ph.foxdrop.core.testing.screenshot.FoxScreenshotVariant.entries,
        )

    @Test
    fun applyErrors() =
        screen(
            "operator_license_apply_errors",
            OperatorLicenseState.Apply(
                ApplyForm(
                    provinces = LicensePreviewData.provinces,
                    errors = mapOf(
                        ph.foxdrop.core.domain.onboarding.OperatorApplicationRules.Field.Province to
                            ph.foxdrop.core.domain.onboarding.Problem.Required,
                        ph.foxdrop.core.domain.onboarding.OperatorApplicationRules.Field.ValidId to
                            ph.foxdrop.core.domain.onboarding.Problem.Required,
                    ),
                ),
                message = DomainError.Conflict("This province already has an operator."),
            ),
        )

    @Test
    fun noProvinces() = screen("operator_license_no_provinces", OperatorLicenseState.Apply(ApplyForm()))

    @Test
    fun loading() = screen(
        "operator_license_loading",
        OperatorLicenseState.Loading,
        listOf(FoxScreenshotVariant.PhoneLight),
    )

    @Test
    fun error() = screen("operator_license_error", OperatorLicenseState.Failed(DomainError.Network))

    @Test
    fun submitted() =
        screen(
            "operator_license_submitted",
            OperatorLicenseState.Status(LicensePreviewData.application(ApplicationStatus.Submitted, null)),
        )

    @Test
    fun approvedWaitingForPayment() =
        screen(
            "operator_license_pay",
            OperatorLicenseState.Status(
                LicensePreviewData.application(ApplicationStatus.Approved),
                pay = PayState.Waiting,
            ),
            ph.foxdrop.core.testing.screenshot.FoxScreenshotVariant.entries,
        )

    @Test
    fun rejected() =
        screen(
            "operator_license_rejected",
            OperatorLicenseState.Status(
                LicensePreviewData.application(ApplicationStatus.Rejected, null, "Business permit expired."),
            ),
        )

    @Test
    fun licenseCardActive() =
        captureFoxScreenshots("operator_license_card_active", twoThemes) {
            LicenseCard(
                LicenseCardState(
                    Loadable.Ready(LicensePreviewData.license(LicenseStatus.Active, 335)),
                ),
                onRenew = {
                },
                onRetry = {},
            )
        }

    @Test
    fun licenseCardGrace() =
        captureFoxScreenshots("operator_license_card_grace", listOf(FoxScreenshotVariant.PhoneLight)) {
            LicenseCard(
                LicenseCardState(
                    Loadable.Ready(LicensePreviewData.license(LicenseStatus.Grace, -3)),
                    PayState.StillPending,
                ),
                onRenew = {},
                onRetry = {},
            )
        }
}
