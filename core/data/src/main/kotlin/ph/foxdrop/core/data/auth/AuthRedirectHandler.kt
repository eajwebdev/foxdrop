package ph.foxdrop.core.data.auth

import android.content.Intent
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
    fun handle(intent: Intent?) = provider.handleAuthRedirect(intent)
}
