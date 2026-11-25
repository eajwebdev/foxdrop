@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package ph.foxdrop.core.domain.startup

import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import ph.foxdrop.core.common.result.FoxResult
import ph.foxdrop.core.domain.auth.AuthRepository
import ph.foxdrop.core.domain.auth.EmailSignUpOutcome
import ph.foxdrop.core.domain.auth.SessionStatus
import ph.foxdrop.core.domain.preferences.AppPreferencesRepository

class ResolveStartDestinationUseCaseTest {
    private val preferences = InMemoryPreferences()
    private val auth = SessionOnlyAuth()
    private val useCase = ResolveStartDestinationUseCase(preferences, auth)

    @Test
    fun `first launch goes to the intro`() = runTest {
        auth.status.value = SessionStatus.SignedOut
        assertEquals(StartDestination.Intro, useCase())
    }

    @Test
    fun `onboarded but signed out goes to auth`() = runTest {
        preferences.completed.value = true
        auth.status.value = SessionStatus.SignedOut
        assertEquals(StartDestination.Auth, useCase())
    }

    @Test
    fun `signed in goes home`() = runTest {
        preferences.completed.value = true
        auth.status.value = SessionStatus.SignedIn("user-1")
        assertEquals(StartDestination.Home, useCase())
    }

    @Test
    fun `unknown session after the timeout counts as signed out`() = runTest {
        preferences.completed.value = true
        auth.status.value = SessionStatus.Unknown
        assertEquals(StartDestination.Auth, useCase())
    }

    @Test
    fun `waits for the session to load within the timeout`() = runTest {
        preferences.completed.value = true
        auth.status.value = SessionStatus.Unknown
        val result = async { useCase() }
        runCurrent() // the use case is now suspended, waiting for a known session
        auth.status.value = SessionStatus.SignedIn("user-2")
        assertEquals(StartDestination.Home, result.await())
    }

    private class InMemoryPreferences : AppPreferencesRepository {
        val completed = MutableStateFlow(false)
        override val hasCompletedOnboarding: Flow<Boolean> = completed

        override suspend fun setOnboardingCompleted() {
            completed.value = true
        }
    }

    private class SessionOnlyAuth : AuthRepository {
        val status = MutableStateFlow<SessionStatus>(SessionStatus.Unknown)
        override val sessionStatus: Flow<SessionStatus> = status

        override suspend fun requestPhoneOtp(phoneE164: String, fullName: String?) = FoxResult.Success(Unit)

        override suspend fun verifyPhoneOtp(phoneE164: String, code: String) = FoxResult.Success(Unit)

        override suspend fun signInWithEmail(email: String, password: String) = FoxResult.Success(Unit)

        override suspend fun signUpWithEmail(fullName: String, email: String, password: String) =
            FoxResult.Success(EmailSignUpOutcome.SignedIn)

        override suspend fun signOut() = FoxResult.Success(Unit)
    }
}
