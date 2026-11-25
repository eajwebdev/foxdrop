package ph.foxdrop.core.designsystem.splash

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import ph.foxdrop.core.designsystem.theme.FoxDropTheme
import ph.foxdrop.core.designsystem.theme.systemAnimationsOff

/**
 * Shows the branded splash (§11A.1) until [isReady] is true, then plays the exit animation.
 * Call **before** `super.onCreate()`. The caller guarantees [isReady] turns true within 1.5 s.
 */
fun ComponentActivity.installFoxSplash(isReady: () -> Boolean) {
    val splash = installSplashScreen()
    splash.setKeepOnScreenCondition { !isReady() }
    splash.installFoxExitAnimation { systemAnimationsOff() }
}

/** Edge-to-edge window (§11A.3) with the FoxDrop theme and background. Call after `super.onCreate()`. */
fun ComponentActivity.setFoxContent(content: @Composable () -> Unit) {
    enableEdgeToEdge()
    setContent {
        FoxDropTheme {
            Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                content()
            }
        }
    }
}
