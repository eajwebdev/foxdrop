package ph.foxdrop.core.payments

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.net.toUri

/**
 * Opens a payment checkout page (PayMongo, or the local fake checkout) in a Custom Tab, so the user stays in the
 * app's task and comes straight back with the system back button (§10). Falls back to any browser when no Custom
 * Tabs provider is installed. Returns false when nothing can open the page.
 */
object CheckoutLauncher {
    fun open(context: Context, checkoutUrl: String): Boolean {
        val uri = checkoutUrl.toUri()
        if (uri.scheme != "https" && uri.scheme != "http") return false
        return try {
            CustomTabsIntent.Builder().setShowTitle(true).build().launchUrl(context, uri)
            true
        } catch (_: ActivityNotFoundException) {
            try {
                context.startActivity(Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                true
            } catch (_: ActivityNotFoundException) {
                false
            }
        }
    }
}
