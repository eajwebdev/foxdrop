package ph.foxdrop.feature.operatorzonesrates.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable
import ph.foxdrop.feature.operatorzonesrates.RateCardRoute
import ph.foxdrop.feature.operatorzonesrates.ZonesRoute

@Serializable
data object ZonesDestination

@Serializable
data object RateCardDestination

/** Territory screens of the operator (§9.4 steps 3 and 5). */
fun NavGraphBuilder.territoryScreens(onBack: () -> Unit) {
    composable<ZonesDestination> { ZonesRoute(onBack = onBack) }
    composable<RateCardDestination> { RateCardRoute(onBack = onBack) }
}
