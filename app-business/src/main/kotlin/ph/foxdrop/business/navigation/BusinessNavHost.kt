package ph.foxdrop.business.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.serialization.Serializable
import ph.foxdrop.business.home.OperatorHomeScreen
import ph.foxdrop.business.home.StoreHomeScreen
import ph.foxdrop.core.designsystem.motion.FoxNavHost
import ph.foxdrop.core.designsystem.motion.resetTo
import ph.foxdrop.core.domain.startup.HomeDecision
import ph.foxdrop.core.domain.startup.StartDestination
import ph.foxdrop.core.model.FoxDropApp
import ph.foxdrop.feature.auth.RoleGate
import ph.foxdrop.feature.auth.navigation.AuthDestination
import ph.foxdrop.feature.auth.navigation.authScreen
import ph.foxdrop.feature.onboarding.navigation.OnboardingGraph
import ph.foxdrop.feature.onboarding.navigation.onboardingGraph

@Serializable
data object BusinessHomeDestination

/** The business app's screens. Features are wired together here, never inside each other (§16). */
@Composable
fun BusinessNavHost(startDestination: StartDestination, modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    val start: Any =
        when (startDestination) {
            StartDestination.Intro -> OnboardingGraph
            StartDestination.Auth -> AuthDestination
            StartDestination.Home -> BusinessHomeDestination
        }
    FoxNavHost(navController = navController, startDestination = start, modifier = modifier) {
        onboardingGraph(navController, FoxDropApp.Business, onFinished = { navController.resetTo(AuthDestination) })
        authScreen(onSignedIn = { navController.resetTo(BusinessHomeDestination) })
        composable<BusinessHomeDestination> {
            // Role routing (§5): operators and store owners get different homes; anyone else sees an explanation.
            RoleGate(app = FoxDropApp.Business, onSignedOut = {
                navController.resetTo(AuthDestination)
            }) { decision, signOut ->
                when (decision) {
                    is HomeDecision.OperatorHome -> OperatorHomeScreen(onSignOut = signOut)
                    is HomeDecision.StoreHome -> StoreHomeScreen(onSignOut = signOut)
                    else -> Unit // RoleGate shows the account-mismatch screen for every other decision
                }
            }
        }
    }
}
