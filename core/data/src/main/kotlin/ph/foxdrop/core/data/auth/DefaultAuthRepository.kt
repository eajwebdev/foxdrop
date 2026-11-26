package ph.foxdrop.core.data.auth

import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import ph.foxdrop.core.common.result.FoxResult
import ph.foxdrop.core.domain.auth.AuthRepository
import ph.foxdrop.core.domain.auth.SessionStatus
import ph.foxdrop.core.network.auth.AuthRemoteDataSource

/**
 * Sign-in through the FoxDrop API (Laravel + Sanctum). Inputs are normalised here; the server validates everything.
 */
internal class DefaultAuthRepository @Inject constructor(
    private val remote: AuthRemoteDataSource,
) : AuthRepository {
    override val sessionStatus: Flow<SessionStatus> = remote.sessionStatus.distinctUntilChanged()

    override suspend fun requestPhoneOtp(phoneE164: String): FoxResult<Unit> = remote.requestPhoneOtp(phoneE164)

    override suspend fun verifyPhoneOtp(phoneE164: String, code: String, fullName: String?): FoxResult<Unit> =
        remote.verifyPhoneOtp(phoneE164, code.trim(), fullName?.trim()?.takeIf(String::isNotEmpty))

    override suspend fun signInWithEmail(email: String, password: String): FoxResult<Unit> =
        remote.signInWithEmail(email.trim().lowercase(), password)

    override suspend fun signUpWithEmail(
        fullName: String,
        email: String,
        password: String,
    ): FoxResult<Unit> = remote.signUpWithEmail(fullName.trim(), email.trim().lowercase(), password)

    override suspend fun signInWithGoogle(idToken: String, rawNonce: String): FoxResult<Unit> =
        remote.signInWithGoogle(idToken, rawNonce)

    override suspend fun signOut(): FoxResult<Unit> = remote.signOut()
}
