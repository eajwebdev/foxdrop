package ph.foxdrop.core.network.auth

import io.github.jan.supabase.auth.SessionManager
import io.github.jan.supabase.auth.exception.NoSessionFoundException
import io.github.jan.supabase.auth.user.UserSession
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import ph.foxdrop.core.datastore.SecureStore

/**
 * Keeps the Supabase session (access + refresh token) in [SecureStore] — DataStore encrypted with Tink and an
 * Android Keystore key — instead of supabase-kt's default plain SharedPreferences (§17).
 */
@Singleton
class EncryptedSessionManager @Inject constructor(private val secureStore: SecureStore) : SessionManager {
    // supabase-kt requires encodeDefaults = true for stored sessions.
    private val json =
        Json {
            encodeDefaults = true
            ignoreUnknownKeys = true
        }

    override suspend fun saveSession(session: UserSession) {
        secureStore.write(KEY, json.encodeToString(UserSession.serializer(), session))
    }

    /** @throws NoSessionFoundException when nothing (readable) is stored, as the SessionManager contract requires. */
    override suspend fun loadSession(): UserSession {
        val stored = secureStore.read(KEY) ?: throw NoSessionFoundException()
        return try {
            json.decodeFromString(UserSession.serializer(), stored)
        } catch (_: SerializationException) {
            secureStore.remove(KEY)
            throw NoSessionFoundException()
        }
    }

    override suspend fun deleteSession() {
        secureStore.remove(KEY)
    }

    private companion object {
        const val KEY = "supabase_session"
    }
}
