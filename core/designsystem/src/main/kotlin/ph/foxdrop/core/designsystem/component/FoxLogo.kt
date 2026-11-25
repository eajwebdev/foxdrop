package ph.foxdrop.core.designsystem.component

import androidx.compose.foundation.Image
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import ph.foxdrop.core.designsystem.R

private const val DARK_SURFACE_LUMINANCE = 0.5f

@Composable
private fun onDarkSurface(): Boolean = MaterialTheme.colorScheme.surface.luminance() < DARK_SURFACE_LUMINANCE

/**
 * The wordmark bitmap for the current theme. Hoist it above AnimatedContent so switching content does not decode
 * the image again.
 */
@Composable
fun foxWordmarkPainter(): Painter =
    painterResource(if (onDarkSurface()) R.drawable.foxdrop_wordmark_on_dark else R.drawable.foxdrop_wordmark)

/** "FoxDrop — Delivering near you" wordmark (§11: login headers, empty states, receipts). */
@Composable
fun FoxWordmark(
    contentDescription: String?,
    modifier: Modifier = Modifier,
    painter: Painter = foxWordmarkPainter(),
) {
    Image(
        painter = painter,
        contentDescription = contentDescription,
        contentScale = ContentScale.Fit,
        modifier = modifier,
    )
}

/** The fox + parcel mark without text. */
@Composable
fun FoxMark(contentDescription: String?, modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(if (onDarkSurface()) R.drawable.foxdrop_mark_on_dark else R.drawable.foxdrop_mark),
        contentDescription = contentDescription,
        contentScale = ContentScale.Fit,
        modifier = modifier,
    )
}

/** The "FD" emblem used by the Business app. */
@Composable
fun FoxEmblem(contentDescription: String?, modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(R.drawable.foxdrop_emblem),
        contentDescription = contentDescription,
        contentScale = ContentScale.Fit,
        modifier = modifier,
    )
}
