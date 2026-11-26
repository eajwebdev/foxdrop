@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package ph.foxdrop.feature.rideronboarding

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode
import ph.foxdrop.core.common.result.DomainError
import ph.foxdrop.core.domain.onboarding.Problem
import ph.foxdrop.core.domain.onboarding.RiderApplicationRules.Field
import ph.foxdrop.core.domain.partner.RiderStatus
import ph.foxdrop.core.domain.partner.VehicleType
import ph.foxdrop.core.testing.MainDispatcherRule
import ph.foxdrop.core.testing.fake.FakeImageCompressor
import ph.foxdrop.core.testing.fake.FakeProvinceRepository
import ph.foxdrop.core.testing.fake.FakeRiderOnboardingRepository
import ph.foxdrop.core.testing.fake.SampleData
import ph.foxdrop.core.testing.screenshot.FoxScreenshotVariant
import ph.foxdrop.core.testing.screenshot.captureFoxScreenshots
import ph.foxdrop.feature.rideronboarding.RiderOnboardingAction as Action
import ph.foxdrop.feature.rideronboarding.RiderOnboardingState as State

@RunWith(AndroidJUnit4::class)
class RiderOnboardingViewModelTest {
    @get:Rule
    val main = MainDispatcherRule(StandardTestDispatcher())

    private val riders = FakeRiderOnboardingRepository()
    private val provinces = FakeProvinceRepository()
    private val compressor = FakeImageCompressor()

    /** 2026-11-26 in Manila. */
    private val clock = Clock.fixed(Instant.parse("2026-11-26T02:00:00Z"), ZoneOffset.UTC)

    private fun viewModel() = RiderOnboardingViewModel(riders, provinces, compressor).also { it.clock = clock }

    private fun RiderOnboardingViewModel.form() = (state.value as State.Apply).form

    private fun RiderOnboardingViewModel.pick(field: Field) = onAction(
        Action.PhotoPicked(field, Uri.parse("content://${field.name}")),
    )

    @Test
    fun `the checklist ticks each part and a motorcycle needs its papers`() = runTest(main.dispatcher) {
        val vm = viewModel()
        runCurrent()
        assertEquals("neg", vm.form().provinceId)
        assertFalse(vm.form().isComplete(ChecklistStep.PersonalDetails))

        vm.onAction(Action.FullNameChanged("Paolo Reyes"))
        vm.pick(Field.Selfie)
        vm.pick(Field.ValidId)
        runCurrent()
        assertTrue(vm.form().isComplete(ChecklistStep.PersonalDetails))
        assertTrue(vm.form().isComplete(ChecklistStep.ValidId))

        vm.onAction(Action.LicenseNoChanged("n01-23-456789"))
        vm.onAction(Action.LicenseExpiryChanged(LocalDate.of(2026, 11, 26)))
        vm.pick(Field.DriversLicense)
        vm.onAction(Action.Submit)
        runCurrent()
        val errors = vm.form().errors
        assertEquals(Problem.NotInFuture, errors[Field.LicenseExpiry])
        assertEquals(Problem.Required, errors[Field.PlateNo])
        assertEquals(Problem.Required, errors[Field.OrCr])
        assertNull(riders.submitted)

        vm.onAction(Action.LicenseExpiryChanged(LocalDate.of(2028, 1, 31)))
        vm.onAction(Action.PlateChanged("neg 1234"))
        vm.pick(Field.OrCr)
        vm.pick(Field.VehiclePhoto)
        runCurrent()
        assertTrue(ChecklistStep.entries.all(vm.form()::isComplete))
        vm.onAction(Action.Submit)
        runCurrent()

        val sent = checkNotNull(riders.submitted)
        assertEquals("NEG 1234", sent.plateNo)
        assertEquals("N01-23-456789", sent.licenseNo)
        assertEquals("2028-01-31", sent.licenseExpiry)
        assertEquals("or_cr.jpg", sent.orCr?.fileName)
        assertEquals(RiderStatus.DocsReview, (vm.state.value as State.Status).rider.status)
    }

    @Test
    fun `a bicycle needs no plate or vehicle papers`() = runTest(main.dispatcher) {
        val vm = viewModel()
        runCurrent()
        vm.onAction(Action.VehicleSelected(VehicleType.Bicycle))
        vm.onAction(Action.PlateChanged("IGNORED"))
        vm.onAction(Action.FullNameChanged("Lito Cruz"))
        vm.onAction(Action.LicenseNoChanged("N01-23-456789"))
        vm.onAction(Action.LicenseExpiryChanged(LocalDate.of(2027, 6, 1)))
        listOf(Field.Selfie, Field.ValidId, Field.DriversLicense).forEach { vm.pick(it) }
        runCurrent()
        assertTrue(vm.form().isComplete(ChecklistStep.VehicleDetails))
        vm.onAction(Action.Submit)
        runCurrent()

        val sent = checkNotNull(riders.submitted)
        assertEquals(VehicleType.Bicycle, sent.vehicleType)
        assertNull(sent.plateNo)
        assertNull(sent.orCr)
        assertNull(sent.vehiclePhoto)
    }

    @Test
    fun `unreadable photos and server errors are reported`() = runTest(main.dispatcher) {
        compressor.result = null
        val vm = viewModel()
        runCurrent()
        vm.effects.test {
            vm.pick(Field.Selfie)
            runCurrent()
            assertEquals(RiderOnboardingEffect.PhotoUnreadable, awaitItem())
        }
        vm.onAction(Action.PhotoPicked(Field.Selfie, null))
        vm.onAction(Action.PhotoPicked(Field.FullName, Uri.parse("content://not-a-photo")))
        assertTrue(vm.form().photos.isEmpty())

        compressor.result = SampleData.photo
        riders.submitError = DomainError.Conflict("Already registered")
        vm.onAction(Action.VehicleSelected(VehicleType.Bicycle))
        vm.onAction(Action.FullNameChanged("Lito Cruz"))
        vm.onAction(Action.LicenseNoChanged("N01-23-456789"))
        vm.onAction(Action.LicenseExpiryChanged(LocalDate.of(2027, 6, 1)))
        listOf(Field.Selfie, Field.ValidId, Field.DriversLicense).forEach { vm.pick(it) }
        runCurrent()
        vm.onAction(Action.Submit)
        assertTrue((vm.state.value as State.Apply).submitting)
        runCurrent()
        assertEquals(DomainError.Conflict("Already registered"), (vm.state.value as State.Apply).message)
        vm.onAction(Action.DismissMessage)
        assertNull((vm.state.value as State.Apply).message)
    }

    @Test
    fun `a registered rider sees the status, refreshes and can fix a rejection`() = runTest(main.dispatcher) {
        riders.rider = SampleData.myRider(RiderStatus.Rejected, "Selfie is blurry")
        val vm = viewModel()
        runCurrent()
        assertEquals(RiderStatus.Rejected, (vm.state.value as State.Status).rider.status)

        riders.rider = SampleData.myRider(RiderStatus.Approved)
        vm.onAction(Action.Refresh)
        runCurrent()
        assertEquals(RiderStatus.Approved, (vm.state.value as State.Status).rider.status)

        riders.rider = SampleData.myRider(RiderStatus.Rejected, "Selfie is blurry")
        vm.onAction(Action.Refresh)
        runCurrent()
        vm.onAction(Action.FixAndResubmit)
        runCurrent()
        assertEquals("NEG 1234", vm.form().plateNo)
        assertEquals("Selfie is blurry", (vm.state.value as State.Apply).previous?.rejectionReason)
    }

    @Test
    fun `load errors can be retried`() = runTest(main.dispatcher) {
        riders.error = DomainError.Network
        val vm = viewModel()
        runCurrent()
        assertEquals(State.Failed(DomainError.Network), vm.state.value)
        riders.error = null
        vm.onAction(Action.Retry)
        runCurrent()
        assertTrue(vm.state.value is State.Apply)
    }
}

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class RiderOnboardingScreenshotTest {
    private val themes = listOf(FoxScreenshotVariant.PhoneLight, FoxScreenshotVariant.PhoneDark)
    private val light = listOf(FoxScreenshotVariant.PhoneLight)
    private val all = FoxScreenshotVariant.entries

    private fun capture(name: String, state: State, variants: List<FoxScreenshotVariant> = all) =
        captureFoxScreenshots(name, variants) { RiderOnboardingScreen(state, {}, {}, {}, {}, onSignOut = {}) }

    @Test
    fun checklist() = capture("rider_checklist", State.Apply(RiderPreviewData.partial))

    @Test
    fun bicycle() =
        capture(
            "rider_checklist_bicycle",
            State.Apply(
                RiderPreviewData.bicycle,
                message = DomainError.Network,
                previous = RiderPreviewData.rider(RiderStatus.Rejected, "Selfie is blurry"),
            ),
            themes,
        )

    @Test
    fun noProvinces() = capture("rider_no_provinces", State.Apply(RiderForm(provinces = persistentListOf())), light)

    @Test
    fun loading() = capture("rider_loading", State.Loading, light)

    @Test
    fun error() = capture("rider_error", State.Failed(DomainError.Network), light)

    @Test
    fun pending() = capture(
        "rider_status_pending",
        State.Status(RiderPreviewData.rider(RiderStatus.DocsReview)),
        themes,
    )

    @Test
    fun approved() = capture("rider_status_approved", State.Status(RiderPreviewData.rider(RiderStatus.Approved)), light)

    @Test
    fun rejected() = capture(
        "rider_status_rejected",
        State.Status(RiderPreviewData.rider(RiderStatus.Rejected, "Selfie is blurry")),
        light,
    )
}
