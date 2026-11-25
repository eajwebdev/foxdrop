package ph.foxdrop.core.datastore

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.google.crypto.tink.Aead
import java.security.GeneralSecurityException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class SecureStoreTest {
    @get:Rule
    val tmp = TemporaryFolder()

    private val dispatcher = StandardTestDispatcher()
    private val scope = TestScope(dispatcher)

    private fun newDataStore() =
        PreferenceDataStoreFactory.create(scope = scope.backgroundScope) {
            tmp.newFile("secure_${System.nanoTime()}.preferences_pb").also { it.delete() }
        }

    @Test
    fun `round trips values and never stores plaintext`() = scope.runTest {
        val dataStore = newDataStore()
        val store = SecureStore(dataStore, lazyOf(XorAead()), dispatcher)

        store.write("session", "access-token-123")

        assertEquals("access-token-123", store.read("session"))
        val raw = dataStore.data.first()[stringPreferencesKey("session")].orEmpty()
        assertTrue(raw.isNotEmpty())
        assertFalse(raw.contains("access-token"))
    }

    @Test
    fun `value bound to its key cannot be read under another key`() = scope.runTest {
        val dataStore = newDataStore()
        val store = SecureStore(dataStore, lazyOf(XorAead()), dispatcher)
        store.write("a", "secret")
        val raw = checkNotNull(dataStore.data.first()[stringPreferencesKey("a")])
        dataStore.edit { it[stringPreferencesKey("b")] = raw }

        assertNull(store.read("b"))
        // The unreadable value was removed.
        assertNull(dataStore.data.first()[stringPreferencesKey("b")])
    }

    @Test
    fun `remove deletes the value`() = scope.runTest {
        val store = SecureStore(newDataStore(), lazyOf(XorAead()), dispatcher)
        store.write("session", "x")
        store.remove("session")
        assertNull(store.read("session"))
    }

    @Test
    fun `preferences default to not onboarded`() = scope.runTest {
        val prefs = AppPreferencesDataSource(newDataStore())
        assertFalse(prefs.hasCompletedOnboarding.first())
        prefs.setOnboardingCompleted()
        assertTrue(prefs.hasCompletedOnboarding.first())
    }

    /** Test AEAD: XOR plus a checksum of the associated data, enough to prove binding and failure handling. */
    private class XorAead : Aead {
        override fun encrypt(plaintext: ByteArray, associatedData: ByteArray): ByteArray =
            byteArrayOf(associatedData.contentHashCode().toByte()) + plaintext.map { (it.toInt() xor KEY).toByte() }

        override fun decrypt(ciphertext: ByteArray, associatedData: ByteArray): ByteArray {
            if (ciphertext.first() != associatedData.contentHashCode().toByte()) {
                throw GeneralSecurityException("associated data mismatch")
            }
            return ciphertext.drop(1).map { (it.toInt() xor KEY).toByte() }.toByteArray()
        }

        private companion object {
            const val KEY = 0x5A
        }
    }
}
