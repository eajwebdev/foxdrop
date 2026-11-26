package ph.foxdrop.core.network.auth

import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import ph.foxdrop.core.common.result.FoxResult
import ph.foxdrop.core.common.result.map
import ph.foxdrop.core.domain.auth.EmailSignUpOutcome
import ph.foxdrop.core.domain.auth.SessionStatus
import ph.foxdrop.core.network.FoxApiClient
import ph.foxdrop.core.network.di.DeviceName
import ph.foxdrop.core.network.session.ApiUser
import ph.foxdrop.core.network.session.SessionState
import ph.foxdrop.core.network.session.SessionStore
import ph.foxdrop.core.network.session.StoredSession

/** FoxDrop API sign-in calls. Every call returns a [FoxResult]; HTTP exceptions never leave this layer. */
interface AuthRemoteDataSource {
    val sessionStatus: Flow<SessionStatus>

    suspend fun requestPhoneOtp(phoneE164: String, fullName: String?): FoxResult<Unit>

    suspend fun verifyPhoneOtp(phoneE164: String, code: String): FoxResult<Unit>

    suspend fun signInWithEmail(email: String, password: String): FoxResult<Unit>

    suspend fun signUpWithEmail(fullName: String, email: String, password: String): FoxResult<EmailSignUpOutcome>

    suspend fun signInWithGoogle(idToken: String, rawNonce: String): FoxResult<Unit>

    suspend fun signOut(): FoxResult<Unit>
}

/** A sign-in response: `{"data": {user}, "token": "...", "expires_at": "..."}`. */
@Serializable
internal data class SignedInResponse(
    val data: ApiUser,
    val token: String,
    @SerialName("expires_at") val expiresAt: String? = null,
)

@Singleton
internal class ApiAuthRemoteDataSource @Inject constructor(
    private val api: FoxApiClient,
    private val sessionStore: SessionStore,
    @param:DeviceName private val deviceName: String,
) : AuthRemoteDataSource {
    /** The name typed on the sign-up form, sent with the code (the account is created when the code is verified). */
    private val pendingNames = ConcurrentHashMap<String, String>()

    override val sessionStatus: Flow<SessionStatus> =
        flow {
            sessionStore.load()
            emitAll(
                sessionStore.state.map { state ->
                    when (state) {
                        SessionState.Loading -> SessionStatus.Unknown
                        SessionState.Absent -> SessionStatus.SignedOut
                        is SessionState.Present -> SessionStatus.SignedIn(state.session.user.id)
                    }
                },
            )
        }

    override suspend fun requestPhoneOtp(phoneE164: String, fullName: String?): FoxResult<Unit> {
        if (fullName.isNullOrBlank()) pendingNames.remove(phoneE164) else pendingNames[phoneE164] = fullName
        return api.call { http -> http.post("auth/otp/send") { setBody(mapOf("phone" to phoneE164)) } }.map { }
    }

    override suspend fun verifyPhoneOtp(phoneE164: String, code: String): FoxResult<Unit> =
        signIn(
            "auth/otp/verify",
            buildMap {
                put("phone", phoneE164)
                put("code", code)
                pendingNames[phoneE164]?.let { put("full_name", it) }
            },
        ).also { if (it is FoxResult.Success) pendingNames.remove(phoneE164) }

    override suspend fun signInWithEmail(email: String, password: String): FoxResult<Unit> =
        signIn("auth/email/login", mapOf("email" to email, "password" to password))

    /** Accounts are signed in right away; email verification arrives with the email features (Phase 6). */
    override suspend fun signUpWithEmail(
        fullName: String,
        email: String,
        password: String,
    ): FoxResult<EmailSignUpOutcome> =
        signIn("auth/email/register", mapOf("full_name" to fullName, "email" to email, "password" to password))
            .map { EmailSignUpOutcome.SignedIn }

    override suspend fun signInWithGoogle(idToken: String, rawNonce: String): FoxResult<Unit> =
        signIn("auth/google", mapOf("id_token" to idToken, "nonce" to rawNonce))

    /**
     * Always ends the session on this phone. If the server cannot be reached (no signal), the token is forgotten
     * anyway; it expires on the server by itself.
     */
    override suspend fun signOut(): FoxResult<Unit> {
        if (sessionStore.load() != null) api.call { http -> http.post("auth/logout") }
        sessionStore.clear()
        return FoxResult.Success(Unit)
    }

    private suspend fun signIn(path: String, fields: Map<String, String>): FoxResult<Unit> =
        api.call { http ->
            val response = http.post(path) { setBody(fields + ("device_name" to deviceName)) }.body<SignedInResponse>()
            sessionStore.save(StoredSession(response.token, response.expiresAt, response.data))
        }
}
