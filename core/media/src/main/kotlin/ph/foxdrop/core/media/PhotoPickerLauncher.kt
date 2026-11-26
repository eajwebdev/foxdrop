package ph.foxdrop.core.media

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import java.io.File
import java.util.UUID

/** Starts the system photo picker or the camera; the result goes to the callback given to [rememberPhotoPicker]. */
@Stable
class PhotoPickerLauncher internal constructor(
    private val choose: () -> Unit,
    private val take: () -> Unit,
) {
    fun chooseFromGallery() = choose()

    fun takePhoto() = take()
}

/**
 * Photo Picker (no storage permission) and camera capture through a cache file shared with [FileProvider]
 * (no camera permission: the camera app takes the picture). [onPicked] gets null when the user cancels.
 */
@Composable
fun rememberPhotoPicker(onPicked: (Uri?) -> Unit): PhotoPickerLauncher {
    val context = LocalContext.current
    val currentOnPicked by rememberUpdatedState(onPicked)
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }
    val gallery =
        rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> currentOnPicked(uri) }
    val camera =
        rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
            val uri = pendingCameraUri
            pendingCameraUri = null
            // A cancelled capture leaves an empty cache file; a saved one is deleted after compression.
            if (!saved && uri != null) context.contentResolver.delete(uri, null, null)
            currentOnPicked(if (saved) uri else null)
        }
    return remember(context) {
        PhotoPickerLauncher(
            choose = {
                gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
            take = {
                val uri = newCameraUri(context)
                pendingCameraUri = uri
                camera.launch(uri)
            },
        )
    }
}

private fun newCameraUri(context: Context): Uri {
    val dir = File(context.cacheDir, "photos").apply { mkdirs() }
    val file = File(dir, "${UUID.randomUUID()}.jpg")
    return FileProvider.getUriForFile(context, mediaAuthority(context), file)
}
