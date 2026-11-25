@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package ph.foxdrop.feature.auth

import app.cash.turbine.test
import java.security.MessageDigest
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import ph.foxdrop.core.common.result.DomainError
import ph.foxdrop.core.testing.MainDispatcherRule
import ph.foxdrop.core.testing.fake.FakeAuthRepository
import ph.foxdrop.feature.auth.google.GoogleNonce
import ph.foxdrop.feature.auth.sms.extractOtp

class GoogleAndSmsReducerTest {
    private fun reduce(state: AuthState, vararg actions: AuthAction): AuthNext =
        actions.fold(AuthNext(state)) { next, action -> AuthReducer.reduce(next.state, action) }

    @Test
    fun `google without a client id says coming soon`() {
        val next = reduce(AuthState(googleEnabled = false), AuthAction.GoogleClicked)
        assertEquals(AuthMessage.GoogleSoon, next.state.message)
        assertNull(next.command)
    }

    @Test
    fun `google flow launches, then signs in with the token`() {
        val launched = reduce(AuthState(googleEnabled = true), AuthAction.GoogleClicked)
        assertEquals(SubmitStatus.Loading, launched.state.submit)
        assertEquals(AuthCommand.LaunchGoogle, launched.command)

        val token = reduce(launched.state, AuthAction.GoogleCredentialReceived("id-token"))
        assertEquals(AuthCommand.SignInWithGoogle("id-token"), token.command)

        val done = reduce(token.state, AuthAction.GoogleSignedIn(null))
        assertEquals(SubmitStatus.Success, done.state.submit)
        assertEquals(AuthCommand.FinishSignIn, done.command)
    }

    @Test
    fun `google cancel, failure and late tokens`() {
        val loading = AuthState(googleEnabled = true, submit = SubmitStatus.Loading)
        assertEquals(SubmitStatus.Idle, reduce(loading, AuthAction.GoogleCancelled).state.submit)
        assertEquals(AuthMessage.GoogleFailed, reduce(loading, AuthAction.GoogleUnavailable).state.message)
        assertEquals(
            AuthMessage.InvalidCredentials,
            reduce(loading, AuthAction.GoogleSignedIn(DomainError.Unauthorized)).state.message,
        )
        // A token that arrives after the user moved on is ignored.
        assertNull(reduce(AuthState(googleEnabled = true), AuthAction.GoogleCredentialReceived("late")).command)
    }

    @Test
    fun `an SMS code fills the boxes and verifies on the OTP step only`() {
        val otp = AuthState(step = AuthStep.Otp, otpPhone = "+639171234567")
        val next = reduce(otp, AuthAction.SmsCodeReceived("654321"))
        assertEquals("654321", next.state.otp)
        assertEquals(AuthCommand.VerifyOtp("+639171234567", "654321"), next.command)

        assertNull(reduce(AuthState(), AuthAction.SmsCodeReceived("654321")).command)
    }

    @Test
    fun `sms text parsing`() {
        assertEquals(
            "123456",
            extractOtp("Your FoxDrop code is 123456. Do not share it with anyone, including riders."),
        )
        assertNull(extractOtp("Your order FD-NEG-0012345 is on the way"))
        assertNull(extractOtp(null))
    }

    @Test
    fun `nonce hash is sha256 of the raw nonce and never repeats`() {
        val a = GoogleNonce.create()
        val b = GoogleNonce.create()
        assertNotEquals(a.raw, b.raw)
        assertEquals(sha256Hex(a.raw), a.sha256)
        assertTrue(a.raw.length >= 40)
    }
}

class GoogleViewModelTest {
    @get:Rule
    val main = MainDispatcherRule(StandardTestDispatcher())

    @Test
    fun `view model asks the screen to open Google, then signs in with the matching nonce`() = runTest(
        main.dispatcher,
    ) {
        val auth = FakeAuthRepository()
        val vm = AuthViewModel(auth, googleClientId = "client.apps.googleusercontent.com")
        assertTrue(vm.state.value.googleEnabled)
        vm.effect.test {
            vm.onAction(AuthAction.GoogleClicked)
            runCurrent()
            val launch = awaitItem() as AuthEffect.LaunchGoogleSignIn
            assertEquals("client.apps.googleusercontent.com", launch.clientId)

            vm.onAction(AuthAction.GoogleCredentialReceived("id-token"))
            runCurrent()
            val call = auth.calls.last()
            assertTrue(call.startsWith("google:id-token:"))
            val rawNonce = call.removePrefix("google:id-token:")
            assertEquals(launch.hashedNonce, sha256Hex(rawNonce))
            assertEquals(AuthEffect.SignedIn, awaitItem())
        }
    }
}

/** Independent of GoogleNonce, so the test checks the production hashing rather than reusing it. */
private fun sha256Hex(text: String): String =
    MessageDigest.getInstance("SHA-256").digest(text.toByteArray()).joinToString("") { "%02x".format(it) }
