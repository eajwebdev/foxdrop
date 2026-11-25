@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package ph.foxdrop.feature.auth

import app.cash.turbine.test
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import ph.foxdrop.core.common.result.DomainError
import ph.foxdrop.core.domain.auth.SessionStatus
import ph.foxdrop.core.testing.MainDispatcherRule
import ph.foxdrop.core.testing.fake.FakeAuthRepository

class AuthViewModelTest {
    @get:Rule
    val main = MainDispatcherRule(StandardTestDispatcher())

    private val auth = FakeAuthRepository()

    @Test
    fun `phone flow sends code, counts down, verifies and signs in`() = runTest(main.dispatcher) {
        val vm = AuthViewModel(auth)
        vm.effect.test {
            vm.onAction(AuthAction.PhoneChanged("9171234567"))
            vm.onAction(AuthAction.Submit)
            runCurrent()
            assertEquals(AuthStep.Otp, vm.state.value.step)
            assertEquals("otp:+639171234567:null", auth.calls.last())

            advanceTimeBy(5_001)
            assertEquals(AuthReducer.RESEND_SECONDS - 5, vm.state.value.resendSecondsLeft)

            vm.onAction(AuthAction.OtpChanged("123456"))
            vm.onAction(AuthAction.VerifyOtp)
            runCurrent()
            assertEquals(SubmitStatus.Success, vm.state.value.submit)
            assertEquals(AuthEffect.SignedIn, awaitItem())
            advanceUntilIdle()
            assertEquals(0, vm.state.value.resendSecondsLeft)
        }
    }

    @Test
    fun `wrong code shakes and allows retry`() = runTest(main.dispatcher) {
        auth.verifyError = DomainError.Validation("request", "invalid")
        val vm = AuthViewModel(auth)
        vm.onAction(AuthAction.PhoneChanged("9171234567"))
        vm.onAction(AuthAction.Submit)
        runCurrent()
        vm.onAction(AuthAction.OtpChanged("000000"))
        vm.onAction(AuthAction.VerifyOtp)
        runCurrent()
        assertEquals(1, vm.state.value.otpShake)
        assertEquals(AuthMessage.WrongCode, vm.state.value.message)
        assertEquals(SubmitStatus.Idle, vm.state.value.submit)
    }

    @Test
    fun `email sign up with confirmation and email sign in`() = runTest(main.dispatcher) {
        auth.signUpOutcome = ph.foxdrop.core.domain.auth.EmailSignUpOutcome.ConfirmationEmailSent
        val vm = AuthViewModel(auth)
        vm.onAction(AuthAction.ModeChanged(AuthMode.SignUp))
        vm.onAction(AuthAction.MethodChanged(AuthMethod.Email))
        vm.onAction(AuthAction.NameChanged("Juan"))
        vm.onAction(AuthAction.EmailChanged("juan@example.ph"))
        vm.onAction(AuthAction.PasswordChanged("foxdrop2026"))
        vm.onAction(AuthAction.TermsChanged(true))
        vm.onAction(AuthAction.Submit)
        runCurrent()
        assertEquals(AuthMessage.ConfirmEmail, vm.state.value.message)
        assertEquals(AuthMode.Login, vm.state.value.mode)

        vm.effect.test {
            vm.onAction(AuthAction.PasswordChanged("foxdrop2026"))
            vm.onAction(AuthAction.Submit)
            runCurrent()
            assertEquals("signin:juan@example.ph", auth.calls.last())
            assertEquals(AuthEffect.SignedIn, awaitItem())
        }
    }

    @Test
    fun `a session that loads late moves on from the auth screen`() = runTest(main.dispatcher) {
        auth.status.value = SessionStatus.Unknown
        val vm = AuthViewModel(auth)
        vm.effect.test {
            auth.status.value = SessionStatus.SignedIn("restored")
            runCurrent()
            assertEquals(AuthEffect.SignedIn, awaitItem())
            auth.status.value = SessionStatus.SignedIn("again")
            runCurrent()
            expectNoEvents()
        }
    }

    @Test
    fun `session view model reports sign out`() = runTest(main.dispatcher) {
        auth.status.value = SessionStatus.SignedIn("u1")
        val vm = SessionViewModel(auth)
        vm.signedOut.test {
            vm.signOut()
            runCurrent()
            awaitItem()
            assertEquals("signout", auth.calls.last())
            cancelAndIgnoreRemainingEvents()
        }
    }
}
