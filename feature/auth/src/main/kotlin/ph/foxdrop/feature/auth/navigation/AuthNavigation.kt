package ph.foxdrop.feature.auth.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable
import ph.foxdrop.feature.auth.AuthRoute

@Serializable
data object AuthDestination

fun NavGraphBuilder.authScreen(onSignedIn: () -> Unit) {
    composable<AuthDestination> {
        AuthRoute(onSignedIn = onSignedIn)
    }
}
