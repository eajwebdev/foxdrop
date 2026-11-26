@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package ph.foxdrop.feature.auth

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode
import ph.foxdrop.core.common.result.DomainError
import ph.foxdrop.core.common.result.FoxResult
import ph.foxdrop.core.domain.startup.HomeDecision
import ph.foxdrop.core.domain.startup.ResolveHomeUseCase
import ph.foxdrop.core.model.AppRole
import ph.foxdrop.core.model.FoxDropApp
import ph.foxdrop.core.model.RoleGrant
import ph.foxdrop.core.testing.MainDispatcherRule
import ph.foxdrop.core.testing.fake.FakeRoleRepository
import ph.foxdrop.core.testing.screenshot.FoxScreenshotVariant
import ph.foxdrop.core.testing.screenshot.captureFoxScreenshots

class RoleGateViewModelTest {
    @get:Rule
    val main = MainDispatcherRule(StandardTestDispatcher())

    private val roles = FakeRoleRepository()

    private fun gate(app: FoxDropApp) = RoleGateViewModel(ResolveHomeUseCase(roles), app)

    @Test
    fun `resolves the home for the app once`() = runTest(main.dispatcher) {
        roles.result = FoxResult.Success(listOf(RoleGrant(AppRole.Operator, provinceId = "neg")))
        val vm = gate(FoxDropApp.Business)
        assertEquals(RoleGateState.Loading, vm.state.value)
        runCurrent()
        assertEquals(RoleGateState.Ready(HomeDecision.OperatorHome("neg")), vm.state.value)
        assertEquals(1, roles.calls)
    }

    @Test
    fun `errors can be retried`() = runTest(main.dispatcher) {
        roles.result = FoxResult.Failure(DomainError.Network)
        val vm = gate(FoxDropApp.Rider)
        runCurrent()
        assertEquals(RoleGateState.Failed(AuthMessage.Network), vm.state.value)

        roles.result = FoxResult.Success(listOf(RoleGrant(AppRole.Rider, provinceId = "neg")))
        vm.retry()
        runCurrent()
        assertEquals(RoleGateState.Ready(HomeDecision.RiderHome("neg")), vm.state.value)
        assertEquals(2, roles.calls)
    }

    @Test
    fun `an unconfigured backend is reported as such`() = runTest(main.dispatcher) {
        roles.result = FoxResult.Failure(DomainError.NotConfigured)
        val vm = gate(FoxDropApp.Customer)
        runCurrent()
        assertEquals(RoleGateState.Failed(AuthMessage.NotConfigured), vm.state.value)
    }

    @Test
    fun `a missing or rejected session asks for sign-in again instead of a retry loop`() = runTest(main.dispatcher) {
        roles.result = FoxResult.Failure(DomainError.Unauthorized)
        val vm = gate(FoxDropApp.Rider)
        runCurrent()
        assertEquals(RoleGateState.SessionExpired, vm.state.value)
    }
}

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class RoleGateScreenshotTest {
    private fun gate(state: RoleGateState) =
        captureFoxScreenshots(
            name = "role_gate_" + state.javaClass.simpleName.lowercase(),
            variants = listOf(FoxScreenshotVariant.PhoneLight, FoxScreenshotVariant.PhoneDark),
        ) {
            RoleGateContent(state = state, onRetry = {}, onSignOut = {}) { _, _ -> Box { Text("home") } }
        }

    @Test
    fun loading() = gate(RoleGateState.Loading)

    @Test
    fun failed() = gate(RoleGateState.Failed(AuthMessage.Network))

    @Test
    fun adminUseWeb() =
        captureFoxScreenshots("role_gate_admin") {
            AccountMismatchScreen(decision = HomeDecision.AdminUseWeb, onSignOut = {})
        }
}
