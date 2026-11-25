package ph.foxdrop.feature.onboarding.navigation

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import kotlinx.serialization.Serializable
import ph.foxdrop.core.model.FoxDropApp
import ph.foxdrop.feature.onboarding.IntroScreen
import ph.foxdrop.feature.onboarding.LocationPermissionRoute
import ph.foxdrop.feature.onboarding.OnboardingPagerScreen
import ph.foxdrop.feature.onboarding.OnboardingViewModel
import ph.foxdrop.feature.onboarding.asksForLocationOnStart
import ph.foxdrop.feature.onboarding.onboardingPagesFor

@Serializable
data object OnboardingGraph

@Serializable
internal data object IntroDestination

@Serializable
internal data object PagesDestination

@Serializable
internal data object LocationDestination

/** First-launch flow: intro → 3 pages → (location explainer) → [onFinished]. */
fun NavGraphBuilder.onboardingGraph(
    navController: NavController,
    app: FoxDropApp,
    onFinished: () -> Unit,
) {
    navigation<OnboardingGraph>(startDestination = IntroDestination) {
        composable<IntroDestination> {
            IntroScreen(
                onFinished = {
                    navController.navigate(PagesDestination) {
                        popUpTo<IntroDestination> { inclusive = true }
                        launchSingleTop = true
                    }
                },
            )
        }
        composable<PagesDestination> {
            val viewModel: OnboardingViewModel = hiltViewModel()
            OnboardingPagerScreen(
                pages = onboardingPagesFor(app),
                onFinished = {
                    viewModel.complete {
                        if (app.asksForLocationOnStart()) {
                            navController.navigate(LocationDestination) { launchSingleTop = true }
                        } else {
                            onFinished()
                        }
                    }
                },
            )
        }
        composable<LocationDestination> {
            LocationPermissionRoute(app = app, onContinue = onFinished)
        }
    }
}
