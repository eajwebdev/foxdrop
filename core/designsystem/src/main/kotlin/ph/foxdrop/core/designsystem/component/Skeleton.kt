package ph.foxdrop.core.designsystem.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import ph.foxdrop.core.designsystem.theme.FoxShapes
import ph.foxdrop.core.designsystem.theme.FoxSpacing
import ph.foxdrop.core.designsystem.theme.FoxTheme

private const val SHIMMER_MILLIS = 1_200

/**
 * Shimmer sweep position for skeletons, or null when animations are removed. Create it once per loading area and
 * pass it to every [SkeletonBox] so all boxes share one animation.
 */
@Composable
fun rememberShimmer(): State<Float>? {
    if (FoxTheme.motion.reduceMotion) return null
    return rememberInfiniteTransition(label = "shimmer").animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(tween(SHIMMER_MILLIS, easing = LinearEasing), RepeatMode.Restart),
        label = "shimmerProgress",
    )
}

/**
 * A shimmering placeholder block shaped like the real content (§11A.5). The sweep is read in the draw phase only,
 * so it never recomposes. With "Remove animations" on it is a still block.
 */
@Composable
fun SkeletonBox(
    modifier: Modifier = Modifier,
    shape: Shape = FoxShapes.Small,
    shimmer: State<Float>? = rememberShimmer(),
) {
    val colors = FoxTheme.colors
    val base = colors.shimmerBase
    if (shimmer == null) {
        Box(modifier.clip(shape).background(base))
        return
    }
    val gradient = remember(base, colors.shimmerHighlight) { listOf(base, colors.shimmerHighlight, base) }
    Box(
        modifier
            .clip(shape)
            .drawBehind {
                val x = size.width * shimmer.value
                drawRect(
                    Brush.linearGradient(
                        colors = gradient,
                        start = Offset(x - size.width / 2, 0f),
                        end = Offset(x + size.width / 2, size.height),
                    ),
                )
            },
    )
}

/** Loading placeholder for a list of cards (image + two text lines). */
@Composable
fun SkeletonList(
    loadingDescription: String,
    modifier: Modifier = Modifier,
    itemCount: Int = 4,
) {
    val shimmer = rememberShimmer()
    Column(
        modifier = modifier.clearAndSetSemantics { contentDescription = loadingDescription },
        verticalArrangement = Arrangement.spacedBy(FoxSpacing.M),
    ) {
        repeat(itemCount) {
            Row(horizontalArrangement = Arrangement.spacedBy(FoxSpacing.S)) {
                SkeletonBox(Modifier.size(72.dp), FoxShapes.Medium, shimmer)
                Column(
                    Modifier.padding(top = FoxSpacing.XS),
                    verticalArrangement = Arrangement.spacedBy(FoxSpacing.XS),
                ) {
                    SkeletonBox(Modifier.fillMaxWidth(0.7f).height(16.dp), shimmer = shimmer)
                    SkeletonBox(Modifier.fillMaxWidth(0.45f).height(12.dp), shimmer = shimmer)
                    SkeletonBox(Modifier.fillMaxWidth(0.3f).height(12.dp), shimmer = shimmer)
                }
            }
        }
    }
}
