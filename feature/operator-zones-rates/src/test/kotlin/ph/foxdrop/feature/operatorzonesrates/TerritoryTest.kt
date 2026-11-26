@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package ph.foxdrop.feature.operatorzonesrates

import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
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
import ph.foxdrop.core.designsystem.component.Loadable
import ph.foxdrop.core.domain.onboarding.Problem
import ph.foxdrop.core.domain.onboarding.RateCardRules
import ph.foxdrop.core.location.LocationResult
import ph.foxdrop.core.model.GeoPoint
import ph.foxdrop.core.testing.MainDispatcherRule
import ph.foxdrop.core.testing.fake.FakeLocationClient
import ph.foxdrop.core.testing.fake.FakeTerritoryRepository
import ph.foxdrop.core.testing.fake.SampleData
import ph.foxdrop.core.testing.screenshot.FoxScreenshotVariant
import ph.foxdrop.core.testing.screenshot.captureFoxScreenshots

@RunWith(AndroidJUnit4::class)
class ZonesViewModelTest {
    @get:Rule
    val main = MainDispatcherRule(StandardTestDispatcher())

    private val territory = FakeTerritoryRepository()
    private val location = FakeLocationClient()

    private fun ready(vm: ZonesViewModel) = (vm.state.value.content as Loadable.Ready).value

    @Test
    fun `a circle around the province centre is saved and listed`() = runTest(main.dispatcher) {
        val vm = ZonesViewModel(territory, location)
        runCurrent()
        assertEquals(1, ready(vm).zones.size)

        vm.onAction(ZonesAction.AddZone)
        assertEquals(SampleData.province.center, vm.state.value.editor?.center)
        vm.onAction(ZonesAction.Save)
        assertEquals(Problem.Required, vm.state.value.editor?.nameProblem)

        vm.onAction(ZonesAction.NameChanged("Silay City"))
        vm.onAction(ZonesAction.RadiusChanged(7.3f))
        assertEquals(7.5f, vm.state.value.editor?.radiusKm)
        vm.onAction(ZonesAction.Save)
        runCurrent()

        assertNull(vm.state.value.editor)
        assertEquals(listOf("Metro Bacolod", "Silay City"), ready(vm).zones.map { it.name })
        assertEquals(listOf("zone:Silay City:7.5"), territory.calls)
    }

    @Test
    fun `my location asks for permission first, then uses the position`() = runTest(main.dispatcher) {
        location.permitted = false
        val vm = ZonesViewModel(territory, location)
        runCurrent()
        vm.onAction(ZonesAction.AddZone)

        vm.effects.test {
            vm.onAction(ZonesAction.UseMyLocation)
            runCurrent()
            assertEquals(ZonesEffect.RequestLocationPermission, awaitItem())
        }
        vm.onAction(ZonesAction.LocationPermissionResult(false))
        assertEquals(LocationResult.PermissionDenied, vm.state.value.editor?.locationProblem)

        location.permitted = true
        vm.onAction(ZonesAction.LocationPermissionResult(true))
        runCurrent()
        assertEquals(GeoPoint(10.674, 122.953), vm.state.value.editor?.center)
        assertEquals(CenterSource.MyLocation, vm.state.value.editor?.source)

        vm.onAction(ZonesAction.UseProvinceCenter)
        assertEquals(SampleData.province.center, vm.state.value.editor?.center)
        vm.onAction(ZonesAction.DismissEditor)
        assertNull(vm.state.value.editor)
    }

    @Test
    fun `deleting removes the zone, and failures are reported`() = runTest(main.dispatcher) {
        val vm = ZonesViewModel(territory, location)
        runCurrent()
        vm.onAction(ZonesAction.Delete("z1"))
        runCurrent()
        assertTrue(ready(vm).zones.isEmpty())

        territory.writeError = DomainError.Network
        vm.onAction(ZonesAction.AddZone)
        vm.onAction(ZonesAction.NameChanged("North"))
        vm.onAction(ZonesAction.Save)
        runCurrent()
        assertEquals(DomainError.Network, vm.state.value.message)
        assertEquals(false, vm.state.value.editor?.saving)
        vm.onAction(ZonesAction.DismissMessage)
        assertNull(vm.state.value.message)
    }

    @Test
    fun `loading errors can be retried`() = runTest(main.dispatcher) {
        territory.error = DomainError.Network
        val vm = ZonesViewModel(territory, location)
        runCurrent()
        assertEquals(Loadable.Failed(DomainError.Network), vm.state.value.content)
        territory.error = null
        vm.onAction(ZonesAction.Retry)
        runCurrent()
        assertTrue(vm.state.value.content is Loadable.Ready)
    }
}

@RunWith(AndroidJUnit4::class)
class RateCardViewModelTest {
    @get:Rule
    val main = MainDispatcherRule(StandardTestDispatcher())

    private val territory = FakeTerritoryRepository()

    @Test
    fun `the current card loads with a server preview`() = runTest(main.dispatcher) {
        val vm = RateCardViewModel(territory)
        runCurrent()

        assertEquals("40", vm.state.value.fields.baseFare)
        assertEquals("80", vm.state.value.fields.riderSharePercent)
        assertEquals("15", vm.state.value.storeFee)
        val preview = (vm.state.value.preview as Loadable.Ready).value
        assertEquals(listOf(4_000L, 5_000L, 7_000L, 12_000L), preview.map { it.deliveryFeeCentavos })
    }

    @Test
    fun `typing previews again only after a pause`() = runTest(main.dispatcher) {
        val vm = RateCardViewModel(territory)
        runCurrent()
        territory.calls.clear()

        vm.onFieldChanged(RateField.BaseFare, "5")
        vm.onFieldChanged(RateField.BaseFare, "50")
        advanceTimeBy(RateCardViewModel.PREVIEW_DEBOUNCE_MS - 1)
        assertTrue(territory.calls.isEmpty())
        advanceTimeBy(2)
        runCurrent()

        assertEquals(listOf("preview:5000"), territory.calls)
    }

    @Test
    fun `invalid values block publishing and show where`() = runTest(main.dispatcher) {
        val vm = RateCardViewModel(territory)
        runCurrent()
        vm.onFieldChanged(RateField.RiderShare, "150")
        vm.onFieldChanged(RateField.BaseFare, "abc")
        vm.publish()

        assertEquals(Problem.OutOfRange, vm.state.value.problems[RateCardRules.Field.RiderShare])
        assertEquals(Problem.Invalid, vm.state.value.problems[RateCardRules.Field.BaseFare])
        assertTrue(territory.calls.none { it.startsWith("publish") })
    }

    @Test
    fun `publishing a valid card and saving the store fee`() = runTest(main.dispatcher) {
        val vm = RateCardViewModel(territory)
        runCurrent()
        vm.onFieldChanged(RateField.BaseFare, "45.50")
        vm.publish()
        runCurrent()
        assertTrue(vm.state.value.justPublished)
        assertEquals(4_550L, territory.card.baseFareCentavos)

        vm.onStoreFeeChanged("20")
        vm.saveStoreFee()
        runCurrent()
        assertTrue(vm.state.value.storeFeeSaved)
        assertTrue("storeFee:2000" in territory.calls)

        vm.onStoreFeeChanged("5000")
        vm.saveStoreFee()
        assertEquals(Problem.OutOfRange, vm.state.value.storeFeeProblem)
        vm.onStoreFeeChanged("x")
        vm.saveStoreFee()
        assertEquals(Problem.Invalid, vm.state.value.storeFeeProblem)
    }

    @Test
    fun `server errors while saving are shown once`() = runTest(main.dispatcher) {
        territory.writeError = DomainError.Unauthorized
        val vm = RateCardViewModel(territory)
        runCurrent()
        vm.publish()
        runCurrent()
        assertEquals(DomainError.Unauthorized, vm.state.value.message)
        vm.dismissMessage()
        vm.saveStoreFee()
        runCurrent()
        assertEquals(DomainError.Unauthorized, vm.state.value.message)
    }

    @Test
    fun `a failed load can be retried`() = runTest(main.dispatcher) {
        territory.error = DomainError.Network
        val vm = RateCardViewModel(territory)
        runCurrent()
        assertEquals(Loadable.Failed(DomainError.Network), vm.state.value.content)
        territory.error = null
        vm.load()
        runCurrent()
        assertEquals(Loadable.Ready(Unit), vm.state.value.content)
    }
}

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class TerritoryScreenshotTest {
    private val themes = listOf(FoxScreenshotVariant.PhoneLight, FoxScreenshotVariant.PhoneDark)

    @Test
    fun zones() =
        captureFoxScreenshots("territory_zones") {
            ZonesScreen(
                ZonesState(
                    Loadable.Ready(ZonesData(TerritoryPreviewData.zones, TerritoryPreviewData.province)),
                ),
                {
                },
                {},
            )
        }

    @Test
    fun zonesEmpty() =
        captureFoxScreenshots("territory_zones_empty", themes) {
            ZonesScreen(ZonesState(Loadable.Ready(ZonesData(persistentListOf(), TerritoryPreviewData.province))), {
            }, {})
        }

    @Test
    fun zoneEditor() =
        captureFoxScreenshots("territory_zone_editor", themes) {
            ZonesScreen(
                ZonesState(
                    Loadable.Ready(ZonesData(persistentListOf(), TerritoryPreviewData.province)),
                    editor =
                        ZoneEditor(
                            name = "Metro Bacolod",
                            source = CenterSource.MyLocation,
                            radiusKm = 8f,
                            locationProblem = LocationResult.PermissionDenied,
                        ),
                ),
                {},
                {},
            )
        }

    @Test
    fun zonesError() =
        captureFoxScreenshots("territory_zones_error", listOf(FoxScreenshotVariant.PhoneLight)) {
            ZonesScreen(ZonesState(Loadable.Failed(DomainError.Network)), {}, {})
        }

    @Test
    fun rates() =
        captureFoxScreenshots("territory_rates") {
            RateCardScreen(TerritoryPreviewData.rates(), {}, {}, { _, _ -> }, {}, {}, {}, {})
        }

    @Test
    fun ratesInvalidAndLoadingPreview() =
        captureFoxScreenshots("territory_rates_invalid", themes) {
            RateCardScreen(
                TerritoryPreviewData.rates(Loadable.Loading).copy(
                    fields = RateFields.from(
                        ph.foxdrop.core.domain.territory.RateCard.Default,
                    ).copy(riderSharePercent = "150"),
                    problems = mapOf(RateCardRules.Field.RiderShare to Problem.OutOfRange),
                    storeFeeSaved = true,
                ),
                {},
                {},
                { _, _ -> },
                {},
                {},
                {},
                {},
            )
        }

    @Test
    fun ratesLoading() =
        captureFoxScreenshots("territory_rates_loading", listOf(FoxScreenshotVariant.PhoneLight)) {
            RateCardScreen(RateCardState(), {}, {}, { _, _ -> }, {}, {}, {}, {})
        }
}
