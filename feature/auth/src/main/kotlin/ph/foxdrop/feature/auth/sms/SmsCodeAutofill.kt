package ph.foxdrop.feature.auth.sms

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.content.IntentCompat
import com.google.android.gms.auth.api.phone.SmsRetriever
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Status

private val SixDigits = Regex("""(?<!\d)(\d{6})(?!\d)""")

/** The 6-digit code in an SMS such as "Your FoxDrop code is 123456. …", or null. */
fun extractOtp(message: String?): String? = message?.let { SixDigits.find(it)?.groupValues?.get(1) }

/**
 * SMS User Consent API (§11A.2): when the code SMS arrives, Android asks the user "Allow FoxDrop to read this
 * message?" — no SMS permission is needed, and nothing is read without that tap. On consent, [onCode] gets the code.
 * Active only while composed (the OTP step).
 */
@Composable
fun SmsCodeAutofill(onCode: (String) -> Unit) {
    val context = LocalContext.current
    val currentOnCode by rememberUpdatedState(onCode)
    val consent =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                extractOtp(result.data?.getStringExtra(SmsRetriever.EXTRA_SMS_MESSAGE))?.let(currentOnCode)
            }
        }
    DisposableEffect(context) {
        val receiver =
            object : BroadcastReceiver() {
                override fun onReceive(receiverContext: Context, intent: Intent) {
                    if (intent.action != SmsRetriever.SMS_RETRIEVED_ACTION) return
                    val status =
                        intent.extras?.let {
                            IntentCompat.getParcelableExtra(intent, SmsRetriever.EXTRA_STATUS, Status::class.java)
                        }
                    if (status?.statusCode != CommonStatusCodes.SUCCESS) return
                    IntentCompat.getParcelableExtra(intent, SmsRetriever.EXTRA_CONSENT_INTENT, Intent::class.java)
                        ?.let(consent::launch)
                }
            }
        // Play services sends the broadcast, guarded by its SEND_PERMISSION; it is exported to receive it.
        ContextCompat.registerReceiver(
            context,
            receiver,
            IntentFilter(SmsRetriever.SMS_RETRIEVED_ACTION),
            SmsRetriever.SEND_PERMISSION,
            null,
            ContextCompat.RECEIVER_EXPORTED,
        )
        SmsRetriever.getClient(context).startSmsUserConsent(null)
        onDispose { context.unregisterReceiver(receiver) }
    }
}
