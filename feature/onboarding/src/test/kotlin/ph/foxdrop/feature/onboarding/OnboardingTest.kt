@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package ph.foxdrop.feature.onboarding

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode
import ph.foxdrop.core.domain.auth.SessionStatus
import ph.foxdrop.core.domain.startup.ResolveStartDestinationUseCase
import ph.foxdrop.core.domain.startup.StartDestination
import ph.foxdrop.core.model.FoxDropApp
import ph.foxdrop.core.testing.MainDispatcherRule
import ph.foxdrop.core.testing.fake.FakeAppPreferencesRepository
import ph.foxdrop.core.testing.fake.FakeAuthRepository
import ph.foxdrop.core.testing.screenshot.FoxScreenshotVariant
import ph.foxdrop.core.testing.screenshot.captureFoxScreenshots

class OnboardingViewModelTest {
    @get:Rule
    val main = MainDispatcherRule(StandardTestDispatcher())

    @Test
    fun `complete saves the flag before continuing`() = runTest(main.dispatcher) {
        val prefs = FakeAppPreferencesRepository()
        var continued = false
        OnboardingViewModel(prefs).complete { continued = prefs.completed.value }
        advanceUntilIdle()
        assertTrue(continued)
    }

    @Test
    fun `startup resolves the first screen`() = runTest(main.dispatcher) {
        val auth = FakeAuthRepository().apply { status.value = SessionStatus.SignedIn("u1") }
        val vm = StartupViewModel(ResolveStartDestinationUseCase(FakeAppPreferencesRepository(onboarded = true), auth))
        assertEquals(null, vm.startDestination.value)
        advanceUntilIdle()
        assertEquals(StartDestination.Home, vm.startDestination.value)
    }

    @Test
    fun `each app has three pages and only business skips location`() {
        FoxDropApp.entries.forEach { assertEquals(3, onboardingPagesFor(it).size) }
        assertTrue(FoxDropApp.Customer.asksForLocationOnStart())
        assertTrue(FoxDropApp.Rider.asksForLocationOnStart())
        assertFalse(FoxDropApp.Business.asksForLocationOnStart())
    }
}

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class OnboardingScreenshotTest {
    @Test
    fun intro() =
        captureFoxScreenshots(
            "onboarding_intro",
            listOf(FoxScreenshotVariant.PhoneLight, FoxScreenshotVariant.TabletLight),
        ) {
            IntroScreen(onFinished = {})
        }

    @Test
    fun pagesCustomer() = captureFoxScreenshots("onboarding_pages_customer") {
        OnboardingPagerScreen(onboardingPagesFor(FoxDropApp.Customer), onFinished = {})
    }

    @Test
    fun pagesRider() =
        captureFoxScreenshots("onboarding_pages_rider", listOf(FoxScreenshotVariant.PhoneLight)) {
            OnboardingPagerScreen(onboardingPagesFor(FoxDropApp.Rider), onFinished = {})
        }

    @Test
    fun pagesBusiness() =
        captureFoxScreenshots("onboarding_pages_business", listOf(FoxScreenshotVariant.PhoneDark)) {
            OnboardingPagerScreen(onboardingPagesFor(FoxDropApp.Business), onFinished = {})
        }

    @Test
    fun location() =
        captureFoxScreenshots("onboarding_location") {
            LocationPermissionScreen(app = FoxDropApp.Customer, onAllow = {}, onNotNow = {})
        }
}
