package ph.foxdrop.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import ph.foxdrop.core.designsystem.theme.FoxShapes

private val CardBorderWidth = 1.dp

/**
 * The only card style in FoxDrop: flat (no shadow, no tonal elevation) with a soft rounded 1 dp border, for a clean
 * look (owner's design rule, docs/DECISIONS.md 23). Use this instead of Material `Card` / elevated `Surface`.
 */
@Composable
fun FoxCard(
    modifier: Modifier = Modifier,
    shape: Shape = FoxShapes.Medium,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier,
        shape = shape,
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(CardBorderWidth, MaterialTheme.colorScheme.outlineVariant),
        content = content,
    )
}
