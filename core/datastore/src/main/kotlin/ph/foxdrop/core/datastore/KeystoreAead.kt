package ph.foxdrop.core.datastore

import android.content.Context
import com.google.crypto.tink.Aead
import com.google.crypto.tink.KeyTemplates
import com.google.crypto.tink.RegistryConfiguration
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.integration.android.AndroidKeysetManager

private const val KEYSET_NAME = "foxdrop_secure_keyset"
private const val KEYSET_PREFS = "foxdrop_secure_keyset_prefs"
private const val MASTER_KEY_URI = "android-keystore://foxdrop_master_key"

/**
 * Tink AES-256-GCM. The Tink keyset is itself encrypted by a master key that lives in the Android Keystore,
 * so the raw key never sits on disk. Not the deprecated `security-crypto` library (§17).
 * Slow (Keystore I/O): call it off the main thread.
 */
internal fun createKeystoreAead(context: Context): Aead {
    AeadConfig.register()
    return AndroidKeysetManager.Builder()
        .withSharedPref(context, KEYSET_NAME, KEYSET_PREFS)
        .withKeyTemplate(KeyTemplates.get("AES256_GCM"))
        .withMasterKeyUri(MASTER_KEY_URI)
        .build()
        .keysetHandle
        .getPrimitive(RegistryConfiguration.get(), Aead::class.java)
}
