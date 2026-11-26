package ph.foxdrop.feature.operatorlicense.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable
import ph.foxdrop.feature.operatorlicense.OperatorLicenseRoute

@Serializable
data object OperatorLicenseDestination

fun NavGraphBuilder.operatorLicenseScreen(onBack: () -> Unit, onRolesChanged: () -> Unit) {
    composable<OperatorLicenseDestination> {
        OperatorLicenseRoute(onBack = onBack, onRolesChanged = onRolesChanged)
    }
}
