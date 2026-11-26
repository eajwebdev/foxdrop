package ph.foxdrop.feature.operatorpartners.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable
import ph.foxdrop.feature.operatorpartners.RiderReviewRoute
import ph.foxdrop.feature.operatorpartners.StoreReviewRoute

/** The field name is the SavedStateHandle key [ph.foxdrop.feature.operatorpartners.StoreReviewViewModel] reads. */
@Serializable
data class StoreReviewDestination(val storeId: String)

@Serializable
data class RiderReviewDestination(val riderId: String)

/** The review screens; the lists themselves are tabs of the operator home. */
fun NavGraphBuilder.partnerReviewScreens(onBack: () -> Unit) {
    composable<StoreReviewDestination> { StoreReviewRoute(onBack = onBack) }
    composable<RiderReviewDestination> { RiderReviewRoute(onBack = onBack) }
}
