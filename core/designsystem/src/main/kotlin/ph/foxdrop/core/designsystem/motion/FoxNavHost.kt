package ph.foxdrop.core.designsystem.motion

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import ph.foxdrop.core.designsystem.theme.FoxTheme

/** NavHost with FoxDrop's default shared-axis X transitions (§11A.3). Predictive back works with these. */
@Composable
fun FoxNavHost(
    navController: NavHostController,
    startDestination: Any,
    modifier: Modifier = Modifier,
    builder: NavGraphBuilder.() -> Unit,
) {
    val motion = FoxTheme.motion
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
        enterTransition = { motion.sharedAxisXEnter(forward = true) },
        exitTransition = { motion.sharedAxisXExit(forward = true) },
        popEnterTransition = { motion.sharedAxisXEnter(forward = false) },
        popExitTransition = { motion.sharedAxisXExit(forward = false) },
        builder = builder,
    )
}

/** Navigates to [route] and clears the back stack (after onboarding, sign-in, sign-out). */
fun NavHostController.resetTo(route: Any) {
    navigate(route) {
        popUpTo(graph.id) { inclusive = true }
        launchSingleTop = true
    }
}
