@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package ph.foxdrop.business.start

import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import ph.foxdrop.core.common.result.DomainError
import ph.foxdrop.core.designsystem.component.Loadable
import ph.foxdrop.core.domain.partner.StoreStatus
import ph.foxdrop.core.testing.MainDispatcherRule
import ph.foxdrop.core.testing.fake.FakeOperatorLicenseRepository
import ph.foxdrop.core.testing.fake.FakeStoreOnboardingRepository
import ph.foxdrop.core.testing.fake.SampleData

class GetStartedViewModelTest {
    @get:Rule
    val main = MainDispatcherRule(StandardTestDispatcher())

    private val stores = FakeStoreOnboardingRepository()
    private val licenses = FakeOperatorLicenseRepository()

    @Test
    fun `a new account has nothing started`() = runTest(main.dispatcher) {
        val vm = GetStartedViewModel(stores, licenses)
        assertEquals(Loadable.Loading, vm.state.value)
        vm.refresh()
        runCurrent()
        val data = (vm.state.value as Loadable.Ready).value
        assertNull(data.store)
        assertNull(data.application)
    }

    @Test
    fun `existing applications are shown and kept while refreshing`() = runTest(main.dispatcher) {
        stores.stores = mutableListOf(SampleData.myStore(StoreStatus.Submitted))
        licenses.application = SampleData.application()
        val vm = GetStartedViewModel(stores, licenses)
        vm.refresh()
        runCurrent()
        assertEquals("s1", (vm.state.value as Loadable.Ready).value.store?.id)

        vm.refresh()
        assertTrue(vm.state.value is Loadable.Ready)
        runCurrent()
        assertEquals("app-1", (vm.state.value as Loadable.Ready).value.application?.id)
    }

    @Test
    fun `either failure is shown`() = runTest(main.dispatcher) {
        licenses.loadError = DomainError.Network
        val vm = GetStartedViewModel(stores, licenses)
        vm.refresh()
        runCurrent()
        assertEquals(Loadable.Failed(DomainError.Network), vm.state.value)

        licenses.loadError = null
        stores.error = DomainError.Unauthorized
        vm.refresh()
        runCurrent()
        assertEquals(Loadable.Failed(DomainError.Unauthorized), vm.state.value)
    }
}
