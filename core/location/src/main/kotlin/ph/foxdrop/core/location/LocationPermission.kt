package ph.foxdrop.core.location

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState

/**
 * Returns a function that asks for location access and reports whether any location permission was granted.
 * Ask only when the user taps "Use my current location" (§17: permissions in context, never at start).
 */
@Composable
fun rememberLocationPermissionRequest(onResult: (granted: Boolean) -> Unit): () -> Unit {
    val currentOnResult by rememberUpdatedState(onResult)
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
            currentOnResult(results.values.any { it })
        }
    return { launcher.launch(LOCATION_PERMISSIONS) }
}
