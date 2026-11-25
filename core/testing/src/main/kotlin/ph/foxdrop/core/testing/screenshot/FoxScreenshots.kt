package ph.foxdrop.core.testing.screenshot

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.v2.runAndroidComposeUiTest
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.captureRoboImage
import com.github.takahirom.roborazzi.roborazziSystemPropertyOutputDirectory
import org.robolectric.RuntimeEnvironment
import ph.foxdrop.core.designsystem.theme.FoxDropTheme

/** One screenshot configuration (§16: light/dark, phone/tablet, font scale 1.5). */
enum class FoxScreenshotVariant(val qualifiers: String, val darkTheme: Boolean, val fontScale: Float) {
    PhoneLight(RobolectricDeviceQualifiers.Pixel7, darkTheme = false, fontScale = 1f),
    PhoneDark(RobolectricDeviceQualifiers.Pixel7, darkTheme = true, fontScale = 1f),
    PhoneLargeFont(RobolectricDeviceQualifiers.Pixel7, darkTheme = false, fontScale = 1.5f),
    TabletLight(RobolectricDeviceQualifiers.MediumTablet, darkTheme = false, fontScale = 1f),
}

private val options =
    RoborazziOptions(
        // Small anti-aliasing differences between machines must not fail CI.
        compareOptions = RoborazziOptions.CompareOptions(changeThreshold = 0.01f),
    )

/**
 * Captures [content] in every entry of [variants], wrapped in [FoxDropTheme] with motion reduced so animations
 * show their final frame. Each variant runs in a fresh Compose test environment, which also pauses infinite
 * animations (spinners, shimmer) so the capture never waits forever.
 * Images go to `src/test/screenshots/<name>_<variant>.png`.
 * Call from a Robolectric test annotated with `@GraphicsMode(GraphicsMode.Mode.NATIVE)`.
 */
@OptIn(ExperimentalTestApi::class, ExperimentalRoborazziApi::class)
fun captureFoxScreenshots(
    name: String,
    variants: List<FoxScreenshotVariant> = FoxScreenshotVariant.entries,
    content: @Composable () -> Unit,
) {
    variants.forEach { variant ->
        RuntimeEnvironment.setQualifiers(variant.qualifiers)
        RuntimeEnvironment.setQualifiers(if (variant.darkTheme) "+night" else "+notnight")
        RuntimeEnvironment.setFontScale(variant.fontScale)
        runAndroidComposeUiTest<ComponentActivity> {
            setContent {
                FoxDropTheme(darkTheme = variant.darkTheme, reduceMotion = true) {
                    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                        content()
                    }
                }
            }
            onNode(isRoot()).captureRoboImage(
                filePath = "${roborazziSystemPropertyOutputDirectory()}/${name}_${variant.name}.png",
                roborazziOptions = options,
            )
        }
    }
    RuntimeEnvironment.setFontScale(1f)
}
