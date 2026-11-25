package ph.foxdrop.core.designsystem.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList
import ph.foxdrop.core.designsystem.theme.FoxShapes
import ph.foxdrop.core.designsystem.theme.FoxSpacing
import ph.foxdrop.core.designsystem.theme.FoxTheme

/**
 * "Log In | Sign Up" pill switch with a sliding indicator (§11A.2). Tabs share the width equally,
 * so the indicator only animates its offset.
 */
@Composable
fun PillTabSwitch(
    options: ImmutableList<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val motion = FoxTheme.motion
    BoxWithConstraints(
        modifier =
            modifier
                .height(52.dp)
                .clip(FoxShapes.Pill)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .padding(4.dp),
    ) {
        val tabWidth = maxWidth / options.size
        val indicatorOffset by animateDpAsState(tabWidth * selectedIndex, motion.bouncy(), label = "pillOffset")
        Box(
            Modifier
                .offset { IntOffset(indicatorOffset.roundToPx(), 0) }
                .width(tabWidth)
                .fillMaxHeight()
                .clip(FoxShapes.Pill)
                .background(MaterialTheme.colorScheme.primary),
        )
        Row(Modifier.selectableGroup()) {
            options.forEachIndexed { index, label ->
                val selected = index == selectedIndex
                val color by animateColorAsState(
                    if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    motion.standard(),
                    label = "pillText",
                )
                Box(
                    modifier =
                        Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(FoxShapes.Pill)
                            .selectable(selected = selected, role = Role.Tab, onClick = { onSelect(index) })
                            .padding(horizontal = FoxSpacing.XS),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = label, style = MaterialTheme.typography.titleMedium, color = color, maxLines = 1)
                }
            }
        }
    }
}
