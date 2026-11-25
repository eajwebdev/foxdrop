package ph.foxdrop.feature.onboarding

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlin.math.absoluteValue
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.launch
import ph.foxdrop.core.designsystem.component.FoxButton
import ph.foxdrop.core.designsystem.component.FoxTextButton
import ph.foxdrop.core.designsystem.preview.FoxPreviews
import ph.foxdrop.core.designsystem.theme.FoxDropTheme
import ph.foxdrop.core.designsystem.theme.FoxLayout
import ph.foxdrop.core.designsystem.theme.FoxSpacing
import ph.foxdrop.core.designsystem.theme.FoxTheme
import ph.foxdrop.core.model.FoxDropApp

private const val PARALLAX = 0.45f

/** Three-page onboarding with parallax art and an animated page indicator (§11A.1). */
@Composable
fun OnboardingPagerScreen(
    pages: ImmutableList<OnboardingPage>,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
    pagerState: PagerState = rememberPagerState { pages.size },
) {
    val motion = FoxTheme.motion
    val scope = rememberCoroutineScope()
    val isLast = pagerState.currentPage == pages.lastIndex
    Column(
        modifier = modifier.fillMaxSize().safeDrawingPadding().padding(FoxSpacing.L),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            FoxTextButton(text = stringResource(R.string.onboarding_skip), onClick = onFinished)
        }
        HorizontalPager(state = pagerState, modifier = Modifier.weight(1f).fillMaxWidth()) { index ->
            PageContent(
                page = pages[index],
                offset = { (pagerState.currentPage - index + pagerState.currentPageOffsetFraction) },
            )
        }
        PageIndicator(
            count = pages.size,
            selected = pagerState.currentPage,
            modifier = Modifier.padding(vertical = FoxSpacing.L),
        )
        FoxButton(
            text = stringResource(if (isLast) R.string.onboarding_start else R.string.onboarding_next),
            onClick = {
                if (isLast) {
                    onFinished()
                } else {
                    scope.launch {
                        pagerState.animateScrollToPage(pagerState.currentPage + 1, animationSpec = motion.standard())
                    }
                }
            },
            modifier = Modifier.widthIn(max = FoxLayout.MaxButtonWidth).fillMaxWidth(),
        )
    }
}

@Composable
private fun PageContent(
    page: OnboardingPage,
    offset: () -> Float,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier =
                Modifier
                    .size(220.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(listOf(colors.primaryContainer, colors.primary.copy(alpha = 0.35f))),
                    ),
        ) {
            Icon(
                imageVector = page.icon,
                contentDescription = null,
                tint = colors.primary,
                modifier =
                    Modifier
                        .size(112.dp)
                        // Parallax: the art moves slower than the page.
                        .graphicsLayer {
                            val o = offset()
                            translationX = o * size.width * PARALLAX
                            alpha = 1f - o.absoluteValue.coerceIn(0f, 1f) * 0.6f
                        },
            )
        }
        Text(
            text = stringResource(page.title),
            style = MaterialTheme.typography.headlineMedium,
            color = colors.onBackground,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = FoxSpacing.XL).semantics { heading() },
        )
        Text(
            text = stringResource(page.body),
            style = MaterialTheme.typography.bodyLarge,
            color = colors.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = FoxSpacing.S).widthIn(max = FoxLayout.MaxTextWidth),
        )
    }
}

@Composable
private fun PageIndicator(
    count: Int,
    selected: Int,
    modifier: Modifier = Modifier,
) {
    val motion = FoxTheme.motion
    val description = stringResource(R.string.onboarding_page_indicator, selected + 1, count)
    Row(
        modifier = modifier.semantics { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(FoxSpacing.XS),
    ) {
        repeat(count) { index ->
            val active = index == selected
            val width by animateDpAsState(if (active) 28.dp else 8.dp, motion.bouncy(), label = "dotWidth")
            Box(
                Modifier
                    .height(8.dp)
                    .width(width)
                    .clip(CircleShape)
                    .background(
                        if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                    ),
            )
        }
    }
}

@FoxPreviews
@Composable
private fun OnboardingPreview() {
    FoxDropTheme { OnboardingPagerScreen(pages = onboardingPagesFor(FoxDropApp.Customer), onFinished = {}) }
}
