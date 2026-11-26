package ph.foxdrop.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import ph.foxdrop.core.designsystem.theme.FoxShapes
import ph.foxdrop.core.designsystem.theme.FoxSpacing
import ph.foxdrop.core.designsystem.theme.FoxTheme

/** How a status reads at a glance. */
enum class StatusTone { Neutral, Info, Success, Warning, Error }

/** A small rounded label such as "Pending review" or "Active". */
@Composable
fun StatusPill(text: String, tone: StatusTone, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val extended = FoxTheme.colors
    val (background, content) =
        when (tone) {
            StatusTone.Neutral -> colors.surfaceVariant to colors.onSurfaceVariant
            StatusTone.Info -> colors.secondaryContainer to colors.onSecondaryContainer
            StatusTone.Success -> extended.success.copy(alpha = 0.16f) to extended.success
            StatusTone.Warning -> extended.warning.copy(alpha = 0.2f) to contentOnWarning(colors.onSurface)
            StatusTone.Error -> colors.errorContainer to colors.onErrorContainer
        }
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = content,
        modifier = modifier.background(
            background,
            FoxShapes.Pill,
        ).padding(horizontal = FoxSpacing.S, vertical = FoxSpacing.XXS),
    )
}

private fun contentOnWarning(onSurface: Color): Color = onSurface
