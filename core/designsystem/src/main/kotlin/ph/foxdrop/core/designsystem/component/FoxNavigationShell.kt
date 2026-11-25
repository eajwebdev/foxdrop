package ph.foxdrop.core.designsystem.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.togetherWith
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.collections.immutable.ImmutableList
import ph.foxdrop.core.designsystem.motion.fadeThroughEnter
import ph.foxdrop.core.designsystem.motion.fadeThroughExit
import ph.foxdrop.core.designsystem.theme.FoxTheme

@Immutable
data class ShellTab(val icon: ImageVector, val label: String)

/**
 * Top-level app navigation (§11A.6): bottom bar on phones, navigation rail on tablets/landscape, drawer on large
 * screens — chosen automatically from the window size. Tabs switch with a fade-through.
 */
@Composable
fun FoxNavigationShell(
    tabs: ImmutableList<ShellTab>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (Int) -> Unit,
) {
    val motion = FoxTheme.motion
    NavigationSuiteScaffold(
        modifier = modifier,
        navigationSuiteItems = {
            tabs.forEachIndexed { index, tab ->
                item(
                    selected = index == selectedIndex,
                    onClick = { onSelect(index) },
                    icon = { Icon(tab.icon, contentDescription = null) },
                    label = { Text(tab.label) },
                )
            }
        },
    ) {
        AnimatedContent(
            targetState = selectedIndex,
            transitionSpec = { motion.fadeThroughEnter() togetherWith motion.fadeThroughExit() },
            label = "shellTab",
        ) { index -> content(index) }
    }
}
