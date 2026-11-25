package ph.foxdrop.core.designsystem.component

import android.view.Window
import android.view.WindowManager
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import ph.foxdrop.core.designsystem.R

/**
 * Blocks screenshots and screen recording while this composable is shown (FLAG_SECURE, §17):
 * OTP, payment, wallet and payout screens.
 *
 * Reference-counted per window: during a transition between two secure screens the outgoing one is disposed after
 * the incoming one starts, and must not clear the flag the incoming one still needs.
 */
@Composable
fun SecureScreen() {
    val window = LocalActivity.current?.window ?: return
    DisposableEffect(window) {
        window.changeSecureCount(+1)
        onDispose { window.changeSecureCount(-1) }
    }
}

private fun Window.changeSecureCount(delta: Int) {
    val view = decorView
    val count = ((view.getTag(R.id.foxdrop_secure_screen_count) as? Int) ?: 0) + delta
    view.setTag(R.id.foxdrop_secure_screen_count, count.coerceAtLeast(0))
    if (count > 0) {
        addFlags(WindowManager.LayoutParams.FLAG_SECURE)
    } else {
        clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
    }
}
