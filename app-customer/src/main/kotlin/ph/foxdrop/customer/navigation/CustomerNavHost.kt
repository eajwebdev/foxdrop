package ph.foxdrop.customer.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.serialization.Serializable
import ph.foxdrop.core.designsystem.motion.FoxNavHost
import ph.foxdrop.core.designsystem.motion.resetTo
import ph.foxdrop.core.domain.startup.StartDestination
import ph.foxdrop.core.model.FoxDropApp
import ph.foxdrop.customer.home.CustomerHomeRoute
import ph.foxdrop.feature.auth.navigation.AuthDestination
import ph.foxdrop.feature.auth.navigation.authScreen
import ph.foxdrop.feature.onboarding.navigation.OnboardingGraph
import ph.foxdrop.feature.onboarding.navigation.onboardingGraph

@Serializable
data object CustomerHomeDestination

/** The customer app's screens. Features are wired together here, never inside each other (§16). */
@Composable
fun CustomerNavHost(startDestination: StartDestination, modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    val start: Any =
        when (startDestination) {
            StartDestination.Intro -> OnboardingGraph
            StartDestination.Auth -> AuthDestination
            StartDestination.Home -> CustomerHomeDestination
        }
    FoxNavHost(navController = navController, startDestination = start, modifier = modifier) {
        onboardingGraph(navController, FoxDropApp.Customer, onFinished = { navController.resetTo(AuthDestination) })
        authScreen(onSignedIn = { navController.resetTo(CustomerHomeDestination) })
        composable<CustomerHomeDestination> {
            CustomerHomeRoute(onSignedOut = { navController.resetTo(AuthDestination) })
        }
    }
}
