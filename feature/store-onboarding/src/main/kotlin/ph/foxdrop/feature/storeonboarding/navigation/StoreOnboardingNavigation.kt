package ph.foxdrop.feature.storeonboarding.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable
import ph.foxdrop.feature.storeonboarding.StoreOnboardingRoute

@Serializable
data object StoreOnboardingDestination

fun NavGraphBuilder.storeOnboardingScreen(onBack: () -> Unit, onRolesChanged: () -> Unit) {
    composable<StoreOnboardingDestination> {
        StoreOnboardingRoute(onBack = onBack, onRolesChanged = onRolesChanged)
    }
}
