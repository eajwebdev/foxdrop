package ph.foxdrop.rider

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import ph.foxdrop.core.designsystem.splash.installFoxSplash
import ph.foxdrop.core.designsystem.splash.setFoxContent
import ph.foxdrop.feature.onboarding.StartupViewModel
import ph.foxdrop.rider.navigation.RiderNavHost

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val startup: StartupViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        installFoxSplash(isReady = { startup.startDestination.value != null })
        super.onCreate(savedInstanceState)
        setFoxContent {
            val start by startup.startDestination.collectAsStateWithLifecycle()
            start?.let { RiderNavHost(startDestination = it) }
        }
    }
}
