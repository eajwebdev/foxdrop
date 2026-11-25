package ph.foxdrop.feature.onboarding

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import ph.foxdrop.core.designsystem.R as DesignR
import ph.foxdrop.core.designsystem.preview.FoxPreviews
import ph.foxdrop.core.designsystem.theme.FoxBrand
import ph.foxdrop.core.designsystem.theme.FoxDropTheme
import ph.foxdrop.core.designsystem.theme.FoxMotion
import ph.foxdrop.core.designsystem.theme.FoxSpacing
import ph.foxdrop.core.designsystem.theme.FoxTheme

private val HOLD = 900.milliseconds

/** Pink speed lines behind the fox: (y fraction, length fraction). Allocated once, read every frame. */
private val SpeedLineStrokes = listOf(0.30f to 0.55f, 0.50f to 0.75f, 0.70f to 0.45f)

/**
 * First-launch brand moment (§11A.1 step 4), built in pure Compose: the fox dashes in leaving pink speed lines,
 * then "FoxDrop — Delivering near you" slides up. Tap anywhere to skip.
 * TODO(design-lottie): swap the Compose animation for the designer's `res/raw/fox_run.json` when it exists.
 */
@Composable
fun IntroScreen(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val motion = FoxTheme.motion
    val start = if (motion.reduceMotion) 1f else 0f
    val dash = remember { Animatable(start) }
    val trail = remember { Animatable(start) }
    val words = remember { Animatable(start) }
    val currentOnFinished by rememberUpdatedState(onFinished)
    // The timer and a tap can both end the intro; only the first one may navigate.
    var finished by remember { mutableStateOf(false) }
    val finish = {
        if (!finished) {
            finished = true
            currentOnFinished()
        }
    }

    LaunchedEffect(Unit) {
        if (!motion.reduceMotion) {
            listOf(
                async { dash.animateTo(1f, motion.dash()) },
                async {
                    delay(FoxMotion.SHORT.milliseconds)
                    trail.animateTo(1f, motion.enter(FoxMotion.LONG))
                },
                async {
                    delay(FoxMotion.MEDIUM.milliseconds * 2)
                    words.animateTo(1f, motion.enter(FoxMotion.LONG))
                },
            ).awaitAll()
        }
        delay(HOLD)
        finish()
    }

    val skipLabel = stringResource(R.string.intro_skip)
    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(FoxBrand.Navy)
                .clickable(role = Role.Button, onClickLabel = skipLabel) { finish() }
                .semantics { contentDescription = skipLabel },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth().height(180.dp)) {
                SpeedLines(progress = { trail.value }, modifier = Modifier.fillMaxWidth().height(120.dp))
                Image(
                    painter = painterResource(DesignR.drawable.foxdrop_mark_on_dark),
                    contentDescription = null,
                    modifier =
                        Modifier
                            .size(width = 220.dp, height = 150.dp)
                            .graphicsLayer {
                                translationX = (1f - dash.value) * -size.width * 2f
                                rotationZ = (1f - dash.value) * -10f
                                alpha = dash.value.coerceIn(0f, 1f)
                            },
                )
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier =
                    Modifier
                        .padding(top = FoxSpacing.M)
                        .graphicsLayer {
                            translationY = (1f - words.value) * 48.dp.toPx()
                            alpha = words.value
                        },
            ) {
                Text(
                    text =
                        buildAnnotatedString {
                            withStyle(SpanStyle(color = FoxBrand.White)) { append("Fox") }
                            withStyle(SpanStyle(color = FoxBrand.Pink)) { append("Drop") }
                        },
                    style = MaterialTheme.typography.displayMedium,
                )
                Text(
                    text = stringResource(R.string.intro_tagline).uppercase(),
                    style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 0.3.em),
                    color = FoxBrand.White,
                )
            }
        }
    }
}

/** Pink motion trail behind the fox. [progress] is read in the draw phase only. */
@Composable
private fun SpeedLines(progress: () -> Float, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val p = progress()
        SpeedLineStrokes.forEachIndexed { index, (yFraction, lengthFraction) ->
            val end = size.width * (0.42f - index * 0.03f)
            val length = size.width * lengthFraction * p
            drawLine(
                color = FoxBrand.Pink.copy(alpha = 0.9f - index * 0.2f),
                start = Offset(end - length, size.height * yFraction),
                end = Offset(end, size.height * yFraction),
                strokeWidth = (8 - index * 2).dp.toPx(),
                cap = StrokeCap.Round,
            )
        }
    }
}

@FoxPreviews
@Composable
private fun IntroPreview() {
    FoxDropTheme(reduceMotion = true) { IntroScreen(onFinished = {}) }
}
