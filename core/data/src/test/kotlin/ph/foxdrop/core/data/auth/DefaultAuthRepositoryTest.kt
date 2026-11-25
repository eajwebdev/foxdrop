package ph.foxdrop.core.data.auth

import app.cash.turbine.test
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import ph.foxdrop.core.common.result.DomainError
import ph.foxdrop.core.common.result.FoxResult
import ph.foxdrop.core.data.preferences.DefaultAppPreferencesRepository
import ph.foxdrop.core.datastore.AppPreferencesDataSource
import ph.foxdrop.core.domain.auth.EmailSignUpOutcome
import ph.foxdrop.core.domain.auth.SessionStatus
import ph.foxdrop.core.network.auth.AuthRemoteDataSource

class DefaultAuthRepositoryTest {
    private val remote = RecordingRemote()
    private val repository = DefaultAuthRepository(remote)

    @Test
    fun `cleans input before calling the backend`() = runTest {
        repository.requestPhoneOtp("+639171234567", "  Juan Dela Cruz ")
        assertEquals("+639171234567|Juan Dela Cruz", remote.calls.last())

        repository.requestPhoneOtp("+639171234567", "   ")
        assertEquals("+639171234567|null", remote.calls.last())

        repository.verifyPhoneOtp("+639171234567", " 123456 ")
        assertEquals("verify|+639171234567|123456", remote.calls.last())

        repository.signInWithEmail(" Juan@Example.PH ", "pass1234")
        assertEquals("signin|juan@example.ph", remote.calls.last())

        val outcome = repository.signUpWithEmail(" Juan ", "Juan@Example.PH", "pass1234")
        assertEquals("signup|Juan|juan@example.ph", remote.calls.last())
        assertEquals(FoxResult.Success(EmailSignUpOutcome.ConfirmationEmailSent), outcome)
    }

    @Test
    fun `passes failures through unchanged`() = runTest {
        remote.failure = DomainError.RateLimited
        assertEquals(FoxResult.Failure(DomainError.RateLimited), repository.requestPhoneOtp("+639171234567"))
        assertEquals(FoxResult.Failure(DomainError.RateLimited), repository.signOut())
    }

    @Test
    fun `session status drops duplicates`() = runTest {
        remote.status.value = SessionStatus.SignedOut
        repository.sessionStatus.test {
            assertEquals(SessionStatus.SignedOut, awaitItem())
            remote.status.value = SessionStatus.SignedOut
            remote.status.value = SessionStatus.SignedIn("u1")
            assertEquals(SessionStatus.SignedIn("u1"), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `preferences repository delegates to data source`() = runTest {
        val dataSource = io.mockk.mockk<AppPreferencesDataSource>(relaxUnitFun = true)
        io.mockk.every { dataSource.hasCompletedOnboarding } returns flowOf(true)
        val prefs = DefaultAppPreferencesRepository(dataSource)
        prefs.hasCompletedOnboarding.test {
            assertEquals(true, awaitItem())
            awaitComplete()
        }
        prefs.setOnboardingCompleted()
        io.mockk.coVerify { dataSource.setOnboardingCompleted() }
    }

    private class RecordingRemote : AuthRemoteDataSource {
        val calls = mutableListOf<String>()
        val status = MutableStateFlow<SessionStatus>(SessionStatus.Unknown)
        var failure: DomainError? = null

        override val sessionStatus: Flow<SessionStatus> = status

        private fun <T> result(value: T): FoxResult<T> =
            failure?.let { FoxResult.Failure(it) } ?: FoxResult.Success(value)

        override suspend fun requestPhoneOtp(phoneE164: String, fullName: String?): FoxResult<Unit> {
            calls += "$phoneE164|$fullName"
            return result(Unit)
        }

        override suspend fun verifyPhoneOtp(phoneE164: String, code: String): FoxResult<Unit> {
            calls += "verify|$phoneE164|$code"
            return result(Unit)
        }

        override suspend fun signInWithEmail(email: String, password: String): FoxResult<Unit> {
            calls += "signin|$email"
            return result(Unit)
        }

        override suspend fun signUpWithEmail(
            fullName: String,
            email: String,
            password: String,
        ): FoxResult<EmailSignUpOutcome> {
            calls += "signup|$fullName|$email"
            return result(EmailSignUpOutcome.ConfirmationEmailSent)
        }

        override suspend fun signOut(): FoxResult<Unit> {
            calls += "signout"
            return result(Unit)
        }
    }
}
