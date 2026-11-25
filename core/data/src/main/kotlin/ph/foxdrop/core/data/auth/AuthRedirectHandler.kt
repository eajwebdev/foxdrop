package ph.foxdrop.core.data.auth

import android.content.Intent
import android.os.Bundle
import javax.inject.Inject
import javax.inject.Singleton
import ph.foxdrop.core.network.SupabaseClientProvider

/**
 * Call from MainActivity.onCreate/onNewIntent: finishes sign-ins that return to the app through
 * `foxdrop-<app>://auth` (email confirmation, OAuth). Other intents are ignored.
 */
@Singleton
class AuthRedirectHandler @Inject internal constructor(
    private val provider: SupabaseClientProvider,
) {
    /** For onNewIntent: always a fresh redirect. */
    fun handle(intent: Intent?) = provider.handleAuthRedirect(intent)

    /**
     * For onCreate: the launch intent is replayed after process death and when the app is reopened from Recents, so a
     * one-time auth code would be exchanged twice. Handle it only on a fresh launch, then drop the data.
     */
    fun handleLaunch(intent: Intent?, savedInstanceState: Bundle?) {
        if (intent == null || savedInstanceState != null) return
        if (intent.flags and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY != 0) return
        provider.handleAuthRedirect(intent)
        intent.data = null
    }
}
