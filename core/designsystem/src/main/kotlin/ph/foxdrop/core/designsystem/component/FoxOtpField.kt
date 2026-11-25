package ph.foxdrop.core.designsystem.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import ph.foxdrop.core.designsystem.theme.FoxShapes
import ph.foxdrop.core.designsystem.theme.FoxSpacing
import ph.foxdrop.core.designsystem.theme.FoxTheme

/**
 * One-time code input shown as separate boxes (§11A.2). It is a single text field underneath, so typing
 * auto-advances and pasting a whole code works. Increase [shakeTrigger] to shake it after a wrong code;
 * set [isSuccess] to turn the boxes green one by one. Used for login OTP and the rider's delivery OTP.
 */
@Composable
fun FoxOtpField(
    value: String,
    onValueChange: (String) -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    length: Int = 6,
    isError: Boolean = false,
    isSuccess: Boolean = false,
    shakeTrigger: Int = 0,
    enabled: Boolean = true,
    onComplete: () -> Unit = {},
) {
    val motion = FoxTheme.motion
    val haptics = LocalHapticFeedback.current
    val shake = remember { Animatable(0f) }
    LaunchedEffect(shakeTrigger) {
        if (shakeTrigger == 0) return@LaunchedEffect
        haptics.performHapticFeedback(HapticFeedbackType.Reject)
        shake.animateTo(0f, motion.shake())
    }

    BasicTextField(
        value = value,
        onValueChange = { input ->
            val digits = input.filter(Char::isDigit).take(length)
            if (digits != value) {
                onValueChange(digits)
                if (digits.length == length) onComplete()
            }
        },
        enabled = enabled,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Done),
        modifier =
            modifier
                .graphicsLayer { translationX = shake.value * density }
                .semantics { this.contentDescription = contentDescription },
        decorationBox = {
            Row(horizontalArrangement = Arrangement.spacedBy(FoxSpacing.XS)) {
                repeat(length) { index ->
                    OtpBox(
                        char = value.getOrNull(index),
                        focused = enabled && index == value.length.coerceAtMost(length - 1),
                        isError = isError,
                        isSuccess = isSuccess,
                        index = index,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        },
    )
}

@Composable
private fun OtpBox(
    char: Char?,
    focused: Boolean,
    isError: Boolean,
    isSuccess: Boolean,
    index: Int,
    modifier: Modifier = Modifier,
) {
    val motion = FoxTheme.motion
    val colors = MaterialTheme.colorScheme
    val target =
        when {
            isSuccess -> FoxTheme.colors.success
            isError -> colors.error
            focused -> colors.primary
            else -> colors.outlineVariant
        }
    // Success turns the boxes green one after another.
    val borderColor by animateColorAsState(
        target,
        motion.standard(delayMillis = if (isSuccess) motion.staggerDelay(index) * 2 else 0),
        label = "otpBorder",
    )
    Box(
        modifier =
            modifier
                .aspectRatio(0.82f)
                .background(colors.surface, FoxShapes.Small)
                .border(if (focused || isError || isSuccess) 2.dp else 1.dp, borderColor, FoxShapes.Small),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = char?.toString().orEmpty(),
            style = MaterialTheme.typography.headlineSmall,
            color = colors.onSurface,
        )
    }
}
