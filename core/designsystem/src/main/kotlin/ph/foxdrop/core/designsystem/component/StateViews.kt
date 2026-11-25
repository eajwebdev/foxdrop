package ph.foxdrop.core.designsystem.component

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ph.foxdrop.core.designsystem.theme.FoxSpacing
import ph.foxdrop.core.designsystem.theme.FoxTheme

/** Friendly empty state: icon, message, optional action (§11A.5). */
@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    StateLayout(
        icon = icon,
        iconTint = MaterialTheme.colorScheme.primary,
        iconBackground = MaterialTheme.colorScheme.primaryContainer,
        title = title,
        message = message,
        modifier = modifier,
        actionLabel = actionLabel,
        onAction = onAction,
    )
}

/** Error with a retry button. The message is a polite live region so TalkBack reads it. */
@Composable
fun ErrorState(
    title: String,
    message: String,
    retryLabel: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Rounded.CloudOff,
) {
    StateLayout(
        icon = icon,
        iconTint = MaterialTheme.colorScheme.error,
        iconBackground = MaterialTheme.colorScheme.errorContainer,
        title = title,
        message = message,
        modifier = modifier.semantics { liveRegion = LiveRegionMode.Polite },
        actionLabel = retryLabel,
        onAction = onRetry,
        actionIcon = Icons.Rounded.Refresh,
    )
}

@Composable
private fun StateLayout(
    icon: ImageVector,
    iconTint: androidx.compose.ui.graphics.Color,
    iconBackground: androidx.compose.ui.graphics.Color,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    actionIcon: ImageVector? = null,
) {
    val motion = FoxTheme.motion
    val appear = remember { Animatable(if (motion.reduceMotion) 1f else 0f) }
    LaunchedEffect(Unit) { appear.animateTo(1f, motion.bouncy()) }
    Column(
        modifier = modifier.fillMaxWidth().padding(FoxSpacing.L),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(FoxSpacing.M),
    ) {
        Box(
            modifier =
                Modifier
                    .size(112.dp)
                    .graphicsLayer {
                        scaleX = 0.8f + 0.2f * appear.value
                        scaleY = 0.8f + 0.2f * appear.value
                        alpha = appear.value
                    }
                    .background(iconBackground, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(56.dp))
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (actionLabel != null && onAction != null) {
            FoxButton(
                text = actionLabel,
                onClick = onAction,
                leadingIcon = actionIcon,
                modifier = Modifier.fillMaxWidth(0.8f).padding(top = FoxSpacing.XS),
            )
        }
    }
}
