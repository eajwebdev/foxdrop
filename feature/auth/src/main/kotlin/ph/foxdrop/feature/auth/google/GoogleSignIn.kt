package ph.foxdrop.feature.auth.google

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

/** Outcome of the Google account picker. */
sealed interface GoogleSignInResult {
    data class Token(val idToken: String) : GoogleSignInResult

    data object Cancelled : GoogleSignInResult

    data object Unavailable : GoogleSignInResult
}

/** One-time nonce: Google signs [sha256] into the ID token; Supabase verifies it against [raw] (replay protection). */
data class GoogleNonce(val raw: String, val sha256: String) {
    companion object {
        private const val BYTES = 32

        fun create(random: SecureRandom = SecureRandom()): GoogleNonce {
            val bytes = ByteArray(BYTES).also(random::nextBytes)
            val raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
            val hash = MessageDigest.getInstance("SHA-256").digest(raw.toByteArray())
            return GoogleNonce(raw, hash.joinToString("") { "%02x".format(it) })
        }
    }
}

/**
 * "Sign in with Google" through Credential Manager (§11A.2 — not the deprecated GoogleSignIn API).
 * [activityContext] must be an Activity so the system sheet can show.
 */
suspend fun requestGoogleIdToken(activityContext: Context, clientId: String, hashedNonce: String): GoogleSignInResult {
    val option = GetSignInWithGoogleOption.Builder(serverClientId = clientId).setNonce(hashedNonce).build()
    val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
    return try {
        val credential = CredentialManager.create(activityContext).getCredential(activityContext, request).credential
        if (credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            GoogleSignInResult.Token(GoogleIdTokenCredential.createFrom(credential.data).idToken)
        } else {
            GoogleSignInResult.Unavailable
        }
    } catch (_: GetCredentialCancellationException) {
        GoogleSignInResult.Cancelled
    } catch (_: GetCredentialException) {
        GoogleSignInResult.Unavailable
    } catch (_: GoogleIdTokenParsingException) {
        GoogleSignInResult.Unavailable
    }
}
