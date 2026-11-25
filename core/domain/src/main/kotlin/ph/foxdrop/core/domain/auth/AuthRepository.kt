package ph.foxdrop.core.domain.auth

import kotlinx.coroutines.flow.Flow
import ph.foxdrop.core.common.result.FoxResult

/** Whether someone is signed in on this device. */
sealed interface SessionStatus {
    data object Unknown : SessionStatus

    data object SignedOut : SessionStatus

    data class SignedIn(val userId: String) : SessionStatus
}

/** What happened after an email sign-up. */
enum class EmailSignUpOutcome {
    /** Signed in right away. */
    SignedIn,

    /** The user must open the confirmation link sent by email first. */
    ConfirmationEmailSent,
}

/**
 * Sign-in and sign-up. Phone OTP (+63) is the primary method (§3). Phone numbers are E.164 (`+639171234567`).
 */
interface AuthRepository {
    val sessionStatus: Flow<SessionStatus>

    suspend fun requestPhoneOtp(phoneE164: String, fullName: String? = null): FoxResult<Unit>

    suspend fun verifyPhoneOtp(phoneE164: String, code: String): FoxResult<Unit>

    suspend fun signInWithEmail(email: String, password: String): FoxResult<Unit>

    suspend fun signUpWithEmail(fullName: String, email: String, password: String): FoxResult<EmailSignUpOutcome>

    /** Google ID token from Credential Manager; [rawNonce] is the unhashed nonce Google signed (replay protection). */
    suspend fun signInWithGoogle(idToken: String, rawNonce: String): FoxResult<Unit>

    suspend fun signOut(): FoxResult<Unit>
}
