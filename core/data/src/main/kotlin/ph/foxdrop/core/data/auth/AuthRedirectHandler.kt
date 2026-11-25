package ph.foxdrop.core.data.auth

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.core.app.OnNewIntentProvider
import javax.inject.Inject
import javax.inject.Singleton
import ph.foxdrop.core.network.SupabaseClientProvider

/**
 * Finishes sign-ins that return to the app through `foxdrop-<app>://auth` (email confirmation, OAuth). Other intents
 * are ignored. One call in `MainActivity.onCreate` covers both the launch intent and later `onNewIntent`s.
 */
@Singleton
class AuthRedirectHandler @Inject internal constructor(
    private val provider: SupabaseClientProvider,
) {
    /**
     * The launch intent is replayed after process death and when the app is reopened from Recents, so a one-time auth
     * code would be exchanged twice: it is handled only on a fresh launch, then its data is dropped.
     */
    fun <A> attach(activity: A, savedInstanceState: Bundle?) where A : Activity, A : OnNewIntentProvider {
        val launch = activity.intent
        val fresh = savedInstanceState == null && launch.flags and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY == 0
        if (fresh && provider.handleAuthRedirect(launch)) launch.data = null
        activity.addOnNewIntentListener { provider.handleAuthRedirect(it) }
    }
}
