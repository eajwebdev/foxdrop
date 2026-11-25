package ph.foxdrop.core.designsystem.motion

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import ph.foxdrop.core.designsystem.theme.FoxMotion

// Material motion patterns for navigation (§11A.3): shared-axis X for forward/back, shared-axis Y for modal flows,
// fade-through for tabs. Durations come from FoxMotion and collapse when animations are removed.

private const val SHARED_AXIS_FRACTION = 10 // incoming/outgoing screens move 1/10 of the width
private const val FADE_THROUGH_SCALE = 0.92f

private fun direction(forward: Boolean) = if (forward) 1 else -1

fun FoxMotion.sharedAxisXEnter(forward: Boolean = true): EnterTransition =
    slideInHorizontally(enter()) { width -> direction(forward) * width / SHARED_AXIS_FRACTION } + fadeIn(enter())

fun FoxMotion.sharedAxisXExit(forward: Boolean = true): ExitTransition =
    slideOutHorizontally(exit()) { width -> -direction(forward) * width / SHARED_AXIS_FRACTION } + fadeOut(exit())

fun FoxMotion.sharedAxisYEnter(): EnterTransition =
    slideInVertically(enter()) { height -> height / SHARED_AXIS_FRACTION } + fadeIn(enter())

fun FoxMotion.sharedAxisYExit(): ExitTransition =
    slideOutVertically(exit()) { height -> height / SHARED_AXIS_FRACTION } + fadeOut(exit())

fun FoxMotion.fadeThroughEnter(): EnterTransition =
    fadeIn(enter(delayMillis = FoxMotion.SHORT / 2)) +
        scaleIn(enter(delayMillis = FoxMotion.SHORT / 2), initialScale = FADE_THROUGH_SCALE)

fun FoxMotion.fadeThroughExit(): ExitTransition = fadeOut(exit())
