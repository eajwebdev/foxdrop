@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package ph.foxdrop.feature.operatorlicense

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
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
import ph.foxdrop.core.common.result.DomainError
import ph.foxdrop.core.domain.onboarding.OperatorApplicationRules.Field
import ph.foxdrop.core.domain.onboarding.Problem
import ph.foxdrop.core.domain.operator.ApplicationStatus
import ph.foxdrop.core.domain.operator.LicenseStatus
import ph.foxdrop.core.domain.operator.PaymentStatus
import ph.foxdrop.core.testing.MainDispatcherRule
import ph.foxdrop.core.testing.fake.FakeImageCompressor
import ph.foxdrop.core.testing.fake.FakeOperatorLicenseRepository
import ph.foxdrop.core.testing.fake.FakeProvinceRepository
import ph.foxdrop.core.testing.fake.SampleData
import ph.foxdrop.feature.operatorlicense.OperatorLicenseAction as Action
import ph.foxdrop.feature.operatorlicense.OperatorLicenseState as State

@RunWith(AndroidJUnit4::class)
class OperatorLicenseViewModelTest {
    @get:Rule
    val main = MainDispatcherRule(StandardTestDispatcher())

    private val licenses = FakeOperatorLicenseRepository()
    private val provinces = FakeProvinceRepository()
    private val compressor = FakeImageCompressor()

    private fun viewModel() = OperatorLicenseViewModel(licenses, provinces, compressor, PaymentPoller(licenses))

    private fun fillForm(vm: OperatorLicenseViewModel) {
        vm.onAction(Action.ProvinceSelected("ilo"))
        vm.onAction(Action.BusinessNameChanged("Gonzaga Logistics"))
        vm.onAction(Action.ContactPhoneChanged("0917 000 0006"))
        vm.onAction(Action.PhotoPicked(DocumentSlot.ValidId, Uri.parse("content://photos/1")))
    }

    @Test
    fun `without an application the form offers only available provinces`() = runTest(main.dispatcher) {
        val vm = viewModel()
        assertEquals(State.Loading, vm.state.value)
        runCurrent()

        val apply = vm.state.value as State.Apply
        assertEquals(listOf("Aklan", "Iloilo"), apply.form.provinces.map { it.name })
        assertNull(apply.previous)
    }

    @Test
    fun `an incomplete form shows what is missing and sends nothing`() = runTest(main.dispatcher) {
        val vm = viewModel()
        runCurrent()
        vm.onAction(Action.Submit)

        val errors = (vm.state.value as State.Apply).form.errors
        assertEquals(Problem.Required, errors[Field.Province])
        assertEquals(Problem.Required, errors[Field.ValidId])
        assertTrue(licenses.calls.isEmpty())

        vm.onAction(Action.ProvinceSelected("ilo"))
        assertNull((vm.state.value as State.Apply).form.errors[Field.Province])
    }

    @Test
    fun `a complete form is submitted and shows the status`() = runTest(main.dispatcher) {
        val vm = viewModel()
        runCurrent()
        fillForm(vm)
        runCurrent()
        vm.onAction(Action.Submit)
        assertTrue((vm.state.value as State.Apply).submitting)
        runCurrent()

        val status = vm.state.value as State.Status
        assertEquals(ApplicationStatus.Submitted, status.application.status)
        assertEquals("+639170000006", licenses.submitted?.contactPhone)
        assertEquals("valid_id.jpg", licenses.submitted?.validId?.fileName)
    }

    @Test
    fun `a taken province shows the server's message and keeps the form`() = runTest(main.dispatcher) {
        licenses.submitError = DomainError.Conflict("This province already has an operator.")
        val vm = viewModel()
        runCurrent()
        fillForm(vm)
        runCurrent()
        vm.onAction(Action.Submit)
        runCurrent()

        val apply = vm.state.value as State.Apply
        assertEquals(DomainError.Conflict("This province already has an operator."), apply.message)
        assertEquals("Gonzaga Logistics", apply.form.businessName)
        vm.onAction(Action.DismissMessage)
        assertNull((vm.state.value as State.Apply).message)
    }

    @Test
    fun `an unreadable photo is reported and not attached`() = runTest(main.dispatcher) {
        compressor.result = null
        val vm = viewModel()
        runCurrent()
        vm.effects.test {
            vm.onAction(Action.PhotoPicked(DocumentSlot.ValidId, Uri.parse("content://photos/broken")))
            runCurrent()
            assertEquals(OperatorLicenseEffect.PhotoUnreadable, awaitItem())
        }
        assertNull((vm.state.value as State.Apply).form.validId)
    }

    @Test
    fun `paying opens the checkout, waits for the payment and re-routes when the license is active`() =
        runTest(main.dispatcher) {
            licenses.application =
                SampleData.application(
                    ApplicationStatus.Approved,
                    SampleData.license(LicenseStatus.PendingPayment, null),
                )
            licenses.payments =
                mutableListOf(SampleData.payment(PaymentStatus.Pending), SampleData.payment(PaymentStatus.Paid))
            val vm = viewModel()
            runCurrent()

            vm.effects.test {
                vm.onAction(Action.Pay)
                runCurrent()
                assertEquals(OperatorLicenseEffect.OpenCheckout("http://10.0.2.2:8000/dev/checkout/pay-1"), awaitItem())
                assertEquals(PayState.Waiting, (vm.state.value as State.Status).pay)

                licenses.application =
                    SampleData.application(ApplicationStatus.Approved, SampleData.license(LicenseStatus.Active))
                advanceTimeBy(4_001)
                runCurrent()
                assertEquals(OperatorLicenseEffect.RolesChanged, awaitItem())
            }
            assertEquals(LicenseStatus.Active, (vm.state.value as State.Status).application.license?.status)
            assertEquals(listOf("pay:lic-1", "poll:pay-1", "poll:pay-1"), licenses.calls)
        }

    @Test
    fun `a declined payment says so`() = runTest(main.dispatcher) {
        licenses.application =
            SampleData.application(ApplicationStatus.Approved, SampleData.license(LicenseStatus.PendingPayment, null))
        licenses.payments = mutableListOf(SampleData.payment(PaymentStatus.Failed))
        val vm = viewModel()
        runCurrent()
        vm.onAction(Action.Pay)
        advanceTimeBy(2_001)
        runCurrent()

        assertEquals(PayState.NotPaid, (vm.state.value as State.Status).pay)
    }

    @Test
    fun `a payment that never arrives stops waiting after two minutes`() = runTest(main.dispatcher) {
        licenses.application =
            SampleData.application(ApplicationStatus.Approved, SampleData.license(LicenseStatus.PendingPayment, null))
        licenses.payments = mutableListOf(SampleData.payment(PaymentStatus.Pending))
        val vm = viewModel()
        runCurrent()
        vm.onAction(Action.Pay)
        advanceTimeBy(PaymentPoller.POLL_DELAYS_MS.sum() + 1)
        runCurrent()

        assertEquals(PayState.StillPending, (vm.state.value as State.Status).pay)
    }

    @Test
    fun `a payment that cannot start shows the error`() = runTest(main.dispatcher) {
        licenses.application =
            SampleData.application(ApplicationStatus.Approved, SampleData.license(LicenseStatus.PendingPayment, null))
        licenses.paymentError = DomainError.Conflict("This license cannot be paid now.")
        val vm = viewModel()
        runCurrent()
        vm.onAction(Action.Pay)
        runCurrent()

        assertEquals(
            PayState.Failed(DomainError.Conflict("This license cannot be paid now.")),
            (vm.state.value as State.Status).pay,
        )
    }

    @Test
    fun `a rejected application can be submitted again`() = runTest(main.dispatcher) {
        licenses.application =
            SampleData.application(ApplicationStatus.Rejected, null).copy(rejectionReason = "Blurry ID")
        val vm = viewModel()
        runCurrent()
        vm.onAction(Action.ApplyAgain)
        runCurrent()

        assertEquals("Blurry ID", (vm.state.value as State.Apply).previous?.rejectionReason)
    }

    @Test
    fun `loading problems can be retried`() = runTest(main.dispatcher) {
        licenses.loadError = DomainError.Network
        val vm = viewModel()
        runCurrent()
        assertEquals(State.Failed(DomainError.Network), vm.state.value)

        licenses.loadError = null
        vm.onAction(Action.Retry)
        runCurrent()
        assertTrue(vm.state.value is State.Apply)
    }

    @Test
    fun `while the admin decides the status re-checks quietly and moves on to payment`() = runTest(main.dispatcher) {
        licenses.application = SampleData.application(ApplicationStatus.Submitted, null)
        val vm = viewModel()
        runCurrent()
        assertTrue((vm.state.value as State.Status).waitingForAdmin)

        // The admin approves in the web; the next automatic check shows it without a spinner.
        licenses.application = SampleData.application(
            ApplicationStatus.Approved,
            SampleData.license(LicenseStatus.PendingPayment, daysLeft = null),
        )
        vm.onAction(Action.Poll)
        assertEquals(false, (vm.state.value as State.Status).refreshing)
        runCurrent()

        val status = vm.state.value as State.Status
        assertEquals(ApplicationStatus.Approved, status.application.status)
        assertTrue(status.waitingForAdmin) // an offline payment may still be recorded by the admin
    }

    @Test
    fun `an active license is not waiting for anyone`() = runTest(main.dispatcher) {
        licenses.application =
            SampleData.application(ApplicationStatus.Approved, SampleData.license(LicenseStatus.Active))
        val vm = viewModel()
        runCurrent()

        assertEquals(false, (vm.state.value as State.Status).waitingForAdmin)
    }
}
