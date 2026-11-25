package ph.foxdrop.core.testing.fake

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import ph.foxdrop.core.common.result.DomainError
import ph.foxdrop.core.common.result.FoxResult
import ph.foxdrop.core.domain.auth.AuthRepository
import ph.foxdrop.core.domain.auth.EmailSignUpOutcome
import ph.foxdrop.core.domain.auth.RoleRepository
import ph.foxdrop.core.domain.auth.SessionStatus
import ph.foxdrop.core.domain.preferences.AppPreferencesRepository
import ph.foxdrop.core.model.AppRole
import ph.foxdrop.core.model.RoleGrant

/** Scriptable [AuthRepository] for ViewModel tests. Records every call in [calls]. */
class FakeAuthRepository : AuthRepository {
    val status = MutableStateFlow<SessionStatus>(SessionStatus.SignedOut)
    override val sessionStatus: Flow<SessionStatus> = status

    val calls = mutableListOf<String>()

    /** Next results; null means success. */
    var requestOtpError: DomainError? = null
    var verifyError: DomainError? = null
    var emailError: DomainError? = null
    var signUpOutcome: EmailSignUpOutcome = EmailSignUpOutcome.SignedIn

    override suspend fun requestPhoneOtp(phoneE164: String, fullName: String?): FoxResult<Unit> {
        calls += "otp:$phoneE164:$fullName"
        return requestOtpError.toResult(Unit)
    }

    override suspend fun verifyPhoneOtp(phoneE164: String, code: String): FoxResult<Unit> {
        calls += "verify:$phoneE164:$code"
        val result = verifyError.toResult(Unit)
        if (result is FoxResult.Success) status.value = SessionStatus.SignedIn("user-phone")
        return result
    }

    override suspend fun signInWithEmail(email: String, password: String): FoxResult<Unit> {
        calls += "signin:$email"
        val result = emailError.toResult(Unit)
        if (result is FoxResult.Success) status.value = SessionStatus.SignedIn("user-email")
        return result
    }

    override suspend fun signUpWithEmail(
        fullName: String,
        email: String,
        password: String,
    ): FoxResult<EmailSignUpOutcome> {
        calls += "signup:$fullName:$email"
        return emailError.toResult(signUpOutcome)
    }

    override suspend fun signInWithGoogle(idToken: String, rawNonce: String): FoxResult<Unit> {
        calls += "google:$idToken:$rawNonce"
        val result = emailError.toResult(Unit)
        if (result is FoxResult.Success) status.value = SessionStatus.SignedIn("user-google")
        return result
    }

    override suspend fun signOut(): FoxResult<Unit> {
        calls += "signout"
        status.value = SessionStatus.SignedOut
        return FoxResult.Success(Unit)
    }

    private fun <T> DomainError?.toResult(value: T): FoxResult<T> =
        if (this == null) FoxResult.Success(value) else FoxResult.Failure(this)
}

/** Scriptable [RoleRepository]: set [result] before the call. */
class FakeRoleRepository(
    var result: FoxResult<List<RoleGrant>> = FoxResult.Success(listOf(RoleGrant(AppRole.Customer))),
) :
    RoleRepository {
    var calls = 0

    override suspend fun myRoles(): FoxResult<List<RoleGrant>> {
        calls++
        return result
    }
}

class FakeAppPreferencesRepository(onboarded: Boolean = false) : AppPreferencesRepository {
    val completed = MutableStateFlow(onboarded)
    override val hasCompletedOnboarding: Flow<Boolean> = completed

    override suspend fun setOnboardingCompleted() {
        completed.value = true
    }
}
