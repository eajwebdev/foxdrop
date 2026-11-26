@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package ph.foxdrop.feature.storeonboarding

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode
import ph.foxdrop.core.common.result.DomainError
import ph.foxdrop.core.domain.onboarding.Problem
import ph.foxdrop.core.domain.onboarding.StoreApplicationRules.Field
import ph.foxdrop.core.domain.partner.StoreCategory
import ph.foxdrop.core.domain.partner.StoreStatus
import ph.foxdrop.core.location.LocationResult
import ph.foxdrop.core.model.GeoPoint
import ph.foxdrop.core.testing.MainDispatcherRule
import ph.foxdrop.core.testing.fake.FakeImageCompressor
import ph.foxdrop.core.testing.fake.FakeLocationClient
import ph.foxdrop.core.testing.fake.FakeProvinceRepository
import ph.foxdrop.core.testing.fake.FakeStoreOnboardingRepository
import ph.foxdrop.core.testing.fake.SampleData
import ph.foxdrop.core.testing.screenshot.FoxScreenshotVariant
import ph.foxdrop.core.testing.screenshot.captureFoxScreenshots
import ph.foxdrop.feature.storeonboarding.StoreOnboardingAction as Action
import ph.foxdrop.feature.storeonboarding.StoreOnboardingState as State

@RunWith(AndroidJUnit4::class)
class StoreOnboardingViewModelTest {
    @get:Rule
    val main = MainDispatcherRule(StandardTestDispatcher())

    private val stores = FakeStoreOnboardingRepository()
    private val provinces = FakeProvinceRepository()
    private val location = FakeLocationClient()
    private val compressor = FakeImageCompressor()

    private fun viewModel() = StoreOnboardingViewModel(stores, provinces, location, compressor)

    private fun StoreOnboardingViewModel.apply() = state.value as State.Apply

    @Test
    fun `a new owner gets the form for serviceable provinces only`() = runTest(main.dispatcher) {
        val vm = viewModel()
        runCurrent()
        val form = vm.apply().form
        assertEquals(listOf("neg"), form.provinces.map { it.id })
        assertEquals("neg", form.provinceId)
    }

    @Test
    fun `a complete application is sent and shows its review status`() = runTest(main.dispatcher) {
        val vm = viewModel()
        runCurrent()
        vm.onAction(Action.Submit)
        val errors = vm.apply().form.errors
        assertEquals(Problem.Required, errors[Field.Name])
        assertEquals(Problem.Required, errors[Field.Location])
        assertEquals(Problem.Required, errors[Field.BusinessPermit])

        vm.onAction(Action.NameChanged("Mama's Kitchen"))
        vm.onAction(Action.CategorySelected(StoreCategory.Groceries))
        vm.onAction(Action.DescriptionChanged("  "))
        vm.onAction(Action.AddressChanged("12 Lacson St."))
        vm.onAction(Action.BarangayChanged("Barangay 17"))
        vm.onAction(Action.CityChanged("Bacolod City"))
        vm.onAction(Action.PhoneChanged("0917 000 0007"))
        vm.onAction(Action.UseMyLocation)
        vm.onAction(Action.PhotoPicked(StoreDocument.BusinessPermit, Uri.parse("content://permit")))
        vm.onAction(Action.PhotoPicked(StoreDocument.ValidId, Uri.parse("content://id")))
        runCurrent()
        assertTrue(vm.apply().form.errors.isEmpty())

        vm.onAction(Action.Submit)
        runCurrent()

        val sent = checkNotNull(stores.submitted)
        assertEquals(StoreCategory.Groceries, sent.category)
        assertNull(sent.description)
        assertEquals("+639170000007", sent.phone)
        assertEquals(GeoPoint(10.674, 122.953), sent.location)
        assertEquals("business_permit.jpg", sent.businessPermit.fileName)
        assertEquals(StoreStatus.Submitted, (vm.state.value as State.Status).store.status)
    }

    @Test
    fun `location asks for permission and explains failures`() = runTest(main.dispatcher) {
        location.permitted = false
        val vm = viewModel()
        runCurrent()
        vm.effects.test {
            vm.onAction(Action.UseMyLocation)
            runCurrent()
            assertEquals(StoreOnboardingEffect.RequestLocationPermission, awaitItem())
        }
        vm.onAction(Action.LocationPermissionResult(false))
        assertEquals(LocationResult.PermissionDenied, vm.apply().form.locationProblem)

        location.permitted = true
        location.result = LocationResult.Disabled
        vm.onAction(Action.LocationPermissionResult(true))
        runCurrent()
        assertEquals(LocationResult.Disabled, vm.apply().form.locationProblem)
        assertNull(vm.apply().form.location)
    }

    @Test
    fun `unreadable photos are reported and server errors keep the form`() = runTest(main.dispatcher) {
        compressor.result = null
        val vm = viewModel()
        runCurrent()
        vm.effects.test {
            vm.onAction(Action.PhotoPicked(StoreDocument.ValidId, Uri.parse("content://broken")))
            runCurrent()
            assertEquals(StoreOnboardingEffect.PhotoUnreadable, awaitItem())
        }
        vm.onAction(Action.PhotoPicked(StoreDocument.ValidId, null))

        compressor.result = SampleData.photo
        stores.submitError = DomainError.Conflict("You already have a store under review")
        vm.onAction(Action.NameChanged("Mama's Kitchen"))
        vm.onAction(Action.AddressChanged("12 Lacson St."))
        vm.onAction(Action.CityChanged("Bacolod City"))
        vm.onAction(Action.UseMyLocation)
        vm.onAction(Action.PhotoPicked(StoreDocument.BusinessPermit, Uri.parse("content://permit")))
        vm.onAction(Action.PhotoPicked(StoreDocument.ValidId, Uri.parse("content://id")))
        runCurrent()
        vm.onAction(Action.Submit)
        runCurrent()
        assertEquals(DomainError.Conflict("You already have a store under review"), vm.apply().message)
        vm.onAction(Action.DismissMessage)
        assertNull(vm.apply().message)
    }

    @Test
    fun `an existing store shows its status, refreshes and can be fixed after a rejection`() = runTest(
        main.dispatcher,
    ) {
        stores.stores = mutableListOf(SampleData.myStore(StoreStatus.Rejected, "Permit is expired"))
        val vm = viewModel()
        runCurrent()
        assertEquals(StoreStatus.Rejected, (vm.state.value as State.Status).store.status)

        stores.stores = mutableListOf(SampleData.myStore(StoreStatus.Active))
        vm.onAction(Action.Refresh)
        assertTrue((vm.state.value as State.Status).refreshing)
        runCurrent()
        assertEquals(StoreStatus.Active, (vm.state.value as State.Status).store.status)

        stores.stores = mutableListOf(SampleData.myStore(StoreStatus.Rejected, "Permit is expired"))
        vm.onAction(Action.Refresh)
        runCurrent()
        vm.onAction(Action.FixAndResubmit)
        runCurrent()
        assertEquals("Mama's Kitchen", vm.apply().form.name)
        assertEquals("Permit is expired", vm.apply().previous?.rejectionReason)
    }

    @Test
    fun `load errors can be retried`() = runTest(main.dispatcher) {
        stores.error = DomainError.Network
        val vm = viewModel()
        runCurrent()
        assertEquals(State.Failed(DomainError.Network), vm.state.value)
        stores.error = null
        provinces.error = DomainError.Network
        vm.onAction(Action.Retry)
        runCurrent()
        assertEquals(State.Failed(DomainError.Network), vm.state.value)
        provinces.error = null
        vm.onAction(Action.Retry)
        runCurrent()
        assertTrue(vm.state.value is State.Apply)
    }
}

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class StoreOnboardingScreenshotTest {
    private val themes = listOf(FoxScreenshotVariant.PhoneLight, FoxScreenshotVariant.PhoneDark)
    private val light = listOf(FoxScreenshotVariant.PhoneLight)
    private val all = FoxScreenshotVariant.entries

    private fun capture(name: String, state: State, variants: List<FoxScreenshotVariant> = all) =
        captureFoxScreenshots(name, variants) { StoreOnboardingScreen(state, {}, {}, {}, {}, {}) }

    @Test
    fun form() = capture("store_form", State.Apply(StorePreviewData.filled))

    @Test
    fun formInvalid() =
        capture(
            "store_form_invalid",
            State.Apply(
                StorePreviewData.invalid.copy(locationProblem = LocationResult.PermissionDenied),
                message = DomainError.Network,
                previous = StorePreviewData.store(StoreStatus.Rejected, "Permit is expired"),
            ),
            themes,
        )

    @Test
    fun noProvinces() = capture("store_no_provinces", State.Apply(StoreForm(provinces = persistentListOf())), light)

    @Test
    fun loading() = capture("store_loading", State.Loading, light)

    @Test
    fun error() = capture("store_error", State.Failed(DomainError.Network), light)

    @Test
    fun submitted() = capture(
        "store_status_submitted",
        State.Status(StorePreviewData.store(StoreStatus.Submitted)),
        themes,
    )

    @Test
    fun active() = capture("store_status_active", State.Status(StorePreviewData.store(StoreStatus.Active)), light)

    @Test
    fun rejected() = capture(
        "store_status_rejected",
        State.Status(StorePreviewData.store(StoreStatus.Rejected, "Permit is expired")),
        light,
    )
}
