package ph.foxdrop.core.designsystem.theme

import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode

val LocalFoxColors = staticCompositionLocalOf { FoxLightExtended }
val LocalFoxMotion = staticCompositionLocalOf { FoxMotion(reduceMotion = false) }

/** Access to FoxDrop tokens that are not part of [MaterialTheme]. */
object FoxTheme {
    val colors: FoxExtendedColors
        @Composable get() = LocalFoxColors.current

    val motion: FoxMotion
        @Composable get() = LocalFoxMotion.current
}

/**
 * The FoxDrop theme for all three apps. Brand colours are fixed (dynamic colour off), light + dark.
 *
 * @param reduceMotion true when the system "Remove animations" setting is on; tests pass it explicitly.
 */
@Composable
fun FoxDropTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    reduceMotion: Boolean = rememberSystemReduceMotion(),
    content: @Composable () -> Unit,
) {
    val motion = remember(reduceMotion) { FoxMotion(reduceMotion) }
    CompositionLocalProvider(
        LocalFoxColors provides if (darkTheme) FoxDarkExtended else FoxLightExtended,
        LocalFoxMotion provides motion,
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) FoxDarkColors else FoxLightColors,
            typography = FoxTypography,
            shapes = FoxMaterialShapes,
            content = content,
        )
    }
}

/** True when Settings › Accessibility › "Remove animations" is on (animator duration scale = 0). */
fun Context.systemAnimationsOff(): Boolean =
    Settings.Global.getFloat(contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f

/** Follows Settings › Accessibility › "Remove animations" (animator duration scale = 0), live. */
@Composable
fun rememberSystemReduceMotion(): Boolean {
    if (LocalInspectionMode.current) return false
    val context = LocalContext.current.applicationContext
    val resolver = context.contentResolver
    var reduce by remember { mutableStateOf(context.systemAnimationsOff()) }
    DisposableEffect(resolver) {
        val observer =
            object : ContentObserver(Handler(Looper.getMainLooper())) {
                override fun onChange(selfChange: Boolean) {
                    reduce = context.systemAnimationsOff()
                }
            }
        resolver.registerContentObserver(
            Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE),
            false,
            observer,
        )
        onDispose { resolver.unregisterContentObserver(observer) }
    }
    return reduce
}
