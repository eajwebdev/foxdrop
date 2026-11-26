package ph.foxdrop.core.media

import android.graphics.BitmapFactory
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddAPhoto
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import ph.foxdrop.core.designsystem.component.FoxCard
import ph.foxdrop.core.designsystem.component.FoxTextButton
import ph.foxdrop.core.designsystem.preview.FoxPreviews
import ph.foxdrop.core.designsystem.theme.FoxDropTheme
import ph.foxdrop.core.designsystem.theme.FoxShapes
import ph.foxdrop.core.designsystem.theme.FoxSpacing
import ph.foxdrop.core.designsystem.theme.FoxTheme
import ph.foxdrop.core.domain.media.PhotoUpload

/**
 * One required document photo (valid ID, driver's license, business permit…): a thumbnail or a placeholder, plus
 * "Take photo" and "Choose photo". The thumbnail fades in when a photo arrives.
 */
@Composable
fun PhotoSlot(
    label: String,
    photo: PhotoUpload?,
    onTakePhoto: () -> Unit,
    onChoosePhoto: () -> Unit,
    modifier: Modifier = Modifier,
    errorMessage: String? = null,
    enabled: Boolean = true,
) {
    val motion = FoxTheme.motion
    val status = stringResource(if (photo == null) R.string.media_photo_missing else R.string.media_photo_added)
    FoxCard(modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(FoxSpacing.S),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(FoxSpacing.S),
        ) {
            AnimatedContent(
                targetState = photo,
                transitionSpec = { fadeIn(motion.enter()) togetherWith fadeOut(motion.exit()) },
                label = "photoSlot",
            ) { current ->
                Thumbnail(current)
            }
            Column(Modifier.weight(1f).semantics { contentDescription = "$label: $status" }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        label,
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (photo != null) {
                        Icon(
                            Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            tint = FoxTheme.colors.success,
                            modifier = Modifier.padding(start = FoxSpacing.XXS).size(18.dp),
                        )
                    }
                }
                if (errorMessage != null) {
                    Text(
                        errorMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                Row {
                    FoxTextButton(stringResource(R.string.media_take_photo), onTakePhoto, enabled = enabled)
                    FoxTextButton(stringResource(R.string.media_choose_photo), onChoosePhoto, enabled = enabled)
                }
            }
        }
    }
}

@Composable
private fun Thumbnail(photo: PhotoUpload?) {
    val bitmap = remember(photo) { photo?.let { BitmapFactory.decodeByteArray(it.bytes, 0, it.bytes.size) } }
    Box(
        modifier = Modifier.size(64.dp).clip(FoxShapes.Small).background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        if (bitmap != null) {
            Image(
                bitmap.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(64.dp),
            )
        } else {
            Icon(Icons.Rounded.AddAPhoto, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@FoxPreviews
@Composable
private fun PhotoSlotPreview() {
    FoxDropTheme {
        PhotoSlot(label = "Valid ID", photo = null, onTakePhoto = {}, onChoosePhoto = {}, errorMessage = "Required")
    }
}
