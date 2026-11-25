package ph.foxdrop.core.designsystem.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ph.foxdrop.core.designsystem.theme.FoxButtonLabel
import ph.foxdrop.core.designsystem.theme.FoxShapes
import ph.foxdrop.core.designsystem.theme.FoxSpacing
import ph.foxdrop.core.designsystem.theme.FoxTheme

/** Idle → Loading → Success morph of the primary button (§11A.2). */
enum class FoxButtonState { Idle, Loading, Success }

private val ButtonHeight = 56.dp

/**
 * Primary pill button ("Sign Up", "Place Order", "Go Online"). While loading it shrinks to a circle with a
 * spinner, then shows a ✓. Clicks are ignored unless [state] is Idle, which prevents double submits.
 */
@Composable
fun FoxButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    state: FoxButtonState = FoxButtonState.Idle,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null,
    loadingDescription: String = text,
) {
    val motion = FoxTheme.motion
    val interaction = remember { MutableInteractionSource() }
    val stateLabel =
        when (state) {
            FoxButtonState.Idle -> null
            FoxButtonState.Loading -> loadingDescription
            FoxButtonState.Success -> text
        }
    BoxWithConstraints(modifier = modifier.height(ButtonHeight), contentAlignment = Alignment.Center) {
        val targetWidth = if (state == FoxButtonState.Idle) maxWidth else ButtonHeight
        val width by animateDpAsState(targetWidth, motion.standard(), label = "buttonWidth")
        Surface(
            onClick = { if (state == FoxButtonState.Idle) onClick() },
            enabled = enabled,
            shape = FoxShapes.Pill,
            color = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            interactionSource = interaction,
            modifier =
                Modifier
                    .width(width)
                    .height(ButtonHeight)
                    .pressScale(interaction)
                    .semantics { stateLabel?.let { stateDescription = it } },
        ) {
            AnimatedContent(
                targetState = state,
                transitionSpec = {
                    (fadeIn(motion.enter()) + scaleIn(motion.enter(), initialScale = 0.6f))
                        .togetherWith(fadeOut(motion.exit()))
                },
                contentAlignment = Alignment.Center,
                label = "buttonContent",
            ) { current ->
                when (current) {
                    FoxButtonState.Idle -> ButtonLabel(text, leadingIcon)

                    FoxButtonState.Loading ->
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 3.dp,
                        )

                    FoxButtonState.Success ->
                        Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(28.dp))
                }
            }
        }
    }
}

@Composable
private fun ButtonLabel(text: String, leadingIcon: ImageVector?) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = FoxSpacing.L),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leadingIcon != null) {
            Icon(leadingIcon, contentDescription = null, modifier = Modifier.padding(end = FoxSpacing.XS))
        }
        Text(text = text, style = FoxButtonLabel, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/**
 * Outlined pill button for secondary actions ("Continue with Google"). Brand logos such as Google's "G" must keep
 * their own colours: pass them with [tintIcon] = false.
 */
@Composable
fun FoxOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: Painter? = null,
    tintIcon: Boolean = true,
) {
    val interaction = remember { MutableInteractionSource() }
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = FoxShapes.Pill,
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        interactionSource = interaction,
        modifier = modifier.height(ButtonHeight).pressScale(interaction),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = FoxSpacing.L),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (leadingIcon != null) {
                Icon(
                    painter = leadingIcon,
                    contentDescription = null,
                    tint = if (tintIcon) LocalContentColor.current else Color.Unspecified,
                    modifier = Modifier.padding(end = FoxSpacing.S).size(20.dp),
                )
            }
            Text(text = text, style = MaterialTheme.typography.titleMedium, maxLines = 1)
        }
    }
}

/** Small text action. Uses navy/secondary in light mode so small text keeps AA contrast. */
@Composable
fun FoxTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    TextButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.secondary,
        )
    }
}
