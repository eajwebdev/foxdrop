package ph.foxdrop.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.google.crypto.tink.Aead
import java.security.GeneralSecurityException
import java.util.Base64
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/**
 * Small encrypted key/value store for secrets such as session tokens (§17).
 * Each value is encrypted with its key as associated data, so values cannot be swapped between keys.
 * If a value cannot be decrypted (for example the Keystore key was wiped), it is deleted and treated as absent.
 *
 * [aead] is lazy: creating it touches the Android Keystore, so it happens on the first read/write (on IO), not at
 * injection time on the main thread.
 */
class SecureStore(
    private val dataStore: DataStore<Preferences>,
    aead: Lazy<Aead>,
    private val ioDispatcher: CoroutineDispatcher,
) {
    private val aead: Aead by aead

    suspend fun read(key: String): String? =
        withContext(ioDispatcher) {
            val stored = dataStore.data.first()[prefKey(key)] ?: return@withContext null
            try {
                val plain = aead.decrypt(Base64.getDecoder().decode(stored), key.toByteArray())
                String(plain, Charsets.UTF_8)
            } catch (_: GeneralSecurityException) {
                remove(key)
                null
            } catch (_: IllegalArgumentException) {
                remove(key)
                null
            }
        }

    suspend fun write(key: String, value: String) {
        withContext(ioDispatcher) {
            val encrypted = aead.encrypt(value.toByteArray(Charsets.UTF_8), key.toByteArray())
            val encoded = Base64.getEncoder().encodeToString(encrypted)
            dataStore.edit { it[prefKey(key)] = encoded }
        }
    }

    suspend fun remove(key: String) {
        withContext(ioDispatcher) {
            dataStore.edit { it.remove(prefKey(key)) }
        }
    }

    private fun prefKey(key: String) = stringPreferencesKey(key)
}
