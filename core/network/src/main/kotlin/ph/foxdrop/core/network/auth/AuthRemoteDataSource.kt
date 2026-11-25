package ph.foxdrop.core.network.auth

import io.github.jan.supabase.auth.OtpType
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.providers.builtin.IDToken
import io.github.jan.supabase.auth.providers.builtin.OTP
import io.github.jan.supabase.auth.status.SessionStatus as SupabaseSessionStatus
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import ph.foxdrop.core.common.result.FoxResult
import ph.foxdrop.core.domain.auth.EmailSignUpOutcome
import ph.foxdrop.core.domain.auth.SessionStatus
import ph.foxdrop.core.network.SupabaseClientProvider

/** Supabase Auth calls. Every call returns a [FoxResult]; Supabase exceptions never leave this class. */
interface AuthRemoteDataSource {
    val sessionStatus: Flow<SessionStatus>

    suspend fun requestPhoneOtp(phoneE164: String, fullName: String?): FoxResult<Unit>

    suspend fun verifyPhoneOtp(phoneE164: String, code: String): FoxResult<Unit>

    suspend fun signInWithEmail(email: String, password: String): FoxResult<Unit>

    suspend fun signUpWithEmail(fullName: String, email: String, password: String): FoxResult<EmailSignUpOutcome>

    suspend fun signInWithGoogle(idToken: String, rawNonce: String): FoxResult<Unit>

    suspend fun signOut(): FoxResult<Unit>
}

@Singleton
internal class SupabaseAuthRemoteDataSource @Inject constructor(
    private val provider: SupabaseClientProvider,
) : AuthRemoteDataSource {
    override val sessionStatus: Flow<SessionStatus> =
        provider.observe(whenNotConfigured = SessionStatus.SignedOut) { client ->
            client.auth.sessionStatus.map { status ->
                when (status) {
                    is SupabaseSessionStatus.Authenticated ->
                        status.session.user?.id?.let(SessionStatus::SignedIn) ?: SessionStatus.SignedOut

                    is SupabaseSessionStatus.NotAuthenticated -> SessionStatus.SignedOut

                    SupabaseSessionStatus.Initializing -> SessionStatus.Unknown

                    // Token refresh failed (often no signal in the province). supabase-kt keeps the stored session and
                    // retries, but exposes no current session meanwhile, so read the user from the encrypted store.
                    // Staying signed in offline matters: a forced re-login costs the user a new SMS OTP.
                    is SupabaseSessionStatus.RefreshFailure ->
                        client.auth.sessionManager.loadSessionOrNull()?.user?.id?.let(SessionStatus::SignedIn)
                            ?: SessionStatus.SignedOut
                }
            }
        }

    override suspend fun requestPhoneOtp(phoneE164: String, fullName: String?): FoxResult<Unit> =
        provider.call { client ->
            client.auth.signInWith(OTP) {
                phone = phoneE164
                createUser = true
                if (!fullName.isNullOrBlank()) data = buildJsonObject { put("full_name", fullName) }
            }
        }

    override suspend fun verifyPhoneOtp(phoneE164: String, code: String): FoxResult<Unit> =
        provider.call { client ->
            client.auth.verifyPhoneOtp(type = OtpType.Phone.SMS, phone = phoneE164, token = code)
        }

    override suspend fun signInWithEmail(email: String, password: String): FoxResult<Unit> =
        provider.call { client ->
            client.auth.signInWith(Email) {
                this.email = email
                this.password = password
            }
        }

    override suspend fun signUpWithEmail(
        fullName: String,
        email: String,
        password: String,
    ): FoxResult<EmailSignUpOutcome> =
        provider.call { client ->
            client.auth.signUpWith(Email) {
                this.email = email
                this.password = password
                data = buildJsonObject { put("full_name", fullName) }
            }
            if (client.auth.currentSessionOrNull() != null) {
                EmailSignUpOutcome.SignedIn
            } else {
                EmailSignUpOutcome.ConfirmationEmailSent
            }
        }

    override suspend fun signInWithGoogle(idToken: String, rawNonce: String): FoxResult<Unit> =
        provider.call { client ->
            client.auth.signInWith(IDToken) {
                this.idToken = idToken
                this.provider = Google
                this.nonce = rawNonce
            }
        }

    /**
     * Always ends the session on this phone. If the server cannot be reached (no signal), the local session is
     * cleared anyway; the server-side refresh token then simply expires.
     */
    override suspend fun signOut(): FoxResult<Unit> {
        val remote = provider.call { client -> client.auth.signOut() }
        if (remote is FoxResult.Success) return remote
        return provider.call { client -> client.auth.clearSession() }
    }
}
