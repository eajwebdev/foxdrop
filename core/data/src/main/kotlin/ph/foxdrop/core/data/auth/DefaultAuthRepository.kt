package ph.foxdrop.core.data.auth

import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import ph.foxdrop.core.common.result.FoxResult
import ph.foxdrop.core.domain.auth.AuthRepository
import ph.foxdrop.core.domain.auth.EmailSignUpOutcome
import ph.foxdrop.core.domain.auth.SessionStatus
import ph.foxdrop.core.network.auth.AuthRemoteDataSource

/**
 * Auth backed by Supabase. Phase 1 adds the profile + role lookup here so the business app can route by role.
 */
internal class DefaultAuthRepository @Inject constructor(
    private val remote: AuthRemoteDataSource,
) : AuthRepository {
    override val sessionStatus: Flow<SessionStatus> = remote.sessionStatus.distinctUntilChanged()

    override suspend fun requestPhoneOtp(phoneE164: String, fullName: String?): FoxResult<Unit> =
        remote.requestPhoneOtp(phoneE164, fullName?.trim()?.takeIf(String::isNotEmpty))

    override suspend fun verifyPhoneOtp(phoneE164: String, code: String): FoxResult<Unit> =
        remote.verifyPhoneOtp(phoneE164, code.trim())

    override suspend fun signInWithEmail(email: String, password: String): FoxResult<Unit> =
        remote.signInWithEmail(email.trim().lowercase(), password)

    override suspend fun signUpWithEmail(
        fullName: String,
        email: String,
        password: String,
    ): FoxResult<EmailSignUpOutcome> = remote.signUpWithEmail(fullName.trim(), email.trim().lowercase(), password)

    override suspend fun signOut(): FoxResult<Unit> = remote.signOut()
}
