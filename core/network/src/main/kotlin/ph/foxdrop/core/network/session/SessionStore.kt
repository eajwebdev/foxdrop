package ph.foxdrop.core.network.session

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import ph.foxdrop.core.datastore.SecureStore

/** Whether a session has been read from storage yet, and what it is. */
sealed interface SessionState {
    data object Loading : SessionState

    data object Absent : SessionState

    data class Present(val session: StoredSession) : SessionState
}

/** The signed-in session on this phone. [state] starts as [SessionState.Loading] until [load] has run once. */
interface SessionStore {
    val state: StateFlow<SessionState>

    /** Reads the stored session once (later calls return the cached value). */
    suspend fun load(): StoredSession?

    suspend fun save(session: StoredSession)

    /**
     * Replaces the cached user (fresh roles from `/me`), but only while the session that asked for them is still the
     * stored one: a sign-out or a new sign-in in the meantime wins.
     */
    suspend fun updateUser(token: String, user: ApiUser)

    suspend fun clear()

    /** Clears the session only if it still uses [token] (a stale request must not sign out a newer session). */
    suspend fun clearIfToken(token: String)
}

/**
 * Keeps the session in [SecureStore]: DataStore encrypted with Tink and an Android Keystore key (§17). The token never
 * touches plain SharedPreferences or logs.
 */
@Singleton
internal class EncryptedSessionStore @Inject constructor(private val secureStore: SecureStore) : SessionStore {
    private val json = Json { ignoreUnknownKeys = true }
    private val mutex = Mutex()
    private val _state = MutableStateFlow<SessionState>(SessionState.Loading)
    override val state: StateFlow<SessionState> = _state.asStateFlow()

    override suspend fun load(): StoredSession? =
        mutex.withLock {
            when (val current = _state.value) {
                is SessionState.Present -> current.session
                SessionState.Absent -> null
                SessionState.Loading -> readStored().also { _state.value = it.toState() }
            }
        }

    override suspend fun save(session: StoredSession) {
        mutex.withLock {
            secureStore.write(KEY, json.encodeToString(StoredSession.serializer(), session))
            _state.value = SessionState.Present(session)
        }
    }

    override suspend fun updateUser(token: String, user: ApiUser) {
        load()
        mutex.withLock {
            val current = (_state.value as? SessionState.Present)?.session ?: return
            if (current.token != token) return
            val updated = current.copy(user = user)
            secureStore.write(KEY, json.encodeToString(StoredSession.serializer(), updated))
            _state.value = SessionState.Present(updated)
        }
    }

    override suspend fun clear() {
        mutex.withLock { clearLocked() }
    }

    override suspend fun clearIfToken(token: String) {
        load()
        mutex.withLock {
            if ((_state.value as? SessionState.Present)?.session?.token == token) clearLocked()
        }
    }

    private suspend fun clearLocked() {
        secureStore.remove(KEY)
        _state.value = SessionState.Absent
    }

    private suspend fun readStored(): StoredSession? {
        val stored = secureStore.read(KEY) ?: return null
        return try {
            json.decodeFromString(StoredSession.serializer(), stored)
        } catch (_: SerializationException) {
            secureStore.remove(KEY)
            null
        } catch (_: IllegalArgumentException) {
            secureStore.remove(KEY)
            null
        }
    }

    private fun StoredSession?.toState(): SessionState = this?.let(SessionState::Present) ?: SessionState.Absent

    private companion object {
        const val KEY = "foxdrop_session"
    }
}
