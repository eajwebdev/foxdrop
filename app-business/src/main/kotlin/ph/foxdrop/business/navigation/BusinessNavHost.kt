package ph.foxdrop.business.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.serialization.Serializable
import ph.foxdrop.business.home.OperatorHomeScreen
import ph.foxdrop.business.home.StoreHomeScreen
import ph.foxdrop.business.start.GetStartedRoute
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
import ph.foxdrop.feature.operatorlicense.LicenseCardRoute
import ph.foxdrop.feature.operatorlicense.navigation.OperatorLicenseDestination
import ph.foxdrop.feature.operatorlicense.navigation.operatorLicenseScreen
import ph.foxdrop.feature.operatorpartners.RiderApprovalsRoute
import ph.foxdrop.feature.operatorpartners.StoreApprovalsRoute
import ph.foxdrop.feature.operatorpartners.navigation.RiderReviewDestination
import ph.foxdrop.feature.operatorpartners.navigation.StoreReviewDestination
import ph.foxdrop.feature.operatorpartners.navigation.partnerReviewScreens
import ph.foxdrop.feature.operatorzonesrates.navigation.RateCardDestination
import ph.foxdrop.feature.operatorzonesrates.navigation.ZonesDestination
import ph.foxdrop.feature.operatorzonesrates.navigation.territoryScreens
import ph.foxdrop.feature.storeonboarding.navigation.StoreOnboardingDestination
import ph.foxdrop.feature.storeonboarding.navigation.storeOnboardingScreen

@Serializable
data object BusinessHomeDestination

/** Set on the home entry when a screen above it granted a role, so the home re-resolves once it is back. */
private const val ROLES_CHANGED = "rolesChanged"

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
    val back: () -> Unit = { navController.popBackStack() }
    val rolesChanged: () -> Unit = { navController.returnHomeWithNewRoles() }
    FoxNavHost(navController = navController, startDestination = start, modifier = modifier) {
        onboardingGraph(navController, FoxDropApp.Business, onFinished = { navController.resetTo(AuthDestination) })
        authScreen(onSignedIn = { navController.resetTo(BusinessHomeDestination) })
        composable<BusinessHomeDestination> { entry ->
            val rolesChangedFlag by entry.savedStateHandle.getStateFlow(
                ROLES_CHANGED,
                false,
            ).collectAsStateWithLifecycle()
            // Role routing (§5, DECISIONS 64): operators and store owners get different homes; anyone else starts here.
            RoleGate(onSignedOut = { navController.resetTo(AuthDestination) }) { home, signOut, refreshRoles ->
                LaunchedEffect(rolesChangedFlag) {
                    if (rolesChangedFlag) {
                        entry.savedStateHandle[ROLES_CHANGED] = false
                        refreshRoles()
                    }
                }
                when (home) {
                    is HomeDecision.OperatorHome ->
                        OperatorHomeScreen(
                            onSignOut = signOut,
                            onOpenZones = { navController.navigate(ZonesDestination) },
                            onOpenRates = { navController.navigate(RateCardDestination) },
                            licenseCard = { LicenseCardRoute() },
                            stores = {
                                StoreApprovalsRoute(onOpenStore = {
                                    navController.navigate(StoreReviewDestination(it))
                                })
                            },
                            riders = {
                                RiderApprovalsRoute(onOpenRider = {
                                    navController.navigate(RiderReviewDestination(it))
                                })
                            },
                        )

                    is HomeDecision.StoreHome -> StoreHomeScreen(onSignOut = signOut)

                    HomeDecision.BusinessOnboarding ->
                        GetStartedRoute(
                            onOpenStore = { navController.navigate(StoreOnboardingDestination) },
                            onBecomeOperator = { navController.navigate(OperatorLicenseDestination) },
                            onSignOut = signOut,
                        )

                    // Never decided in this app.
                    HomeDecision.CustomerHome, is HomeDecision.RiderHome, HomeDecision.RiderOnboarding -> Unit
                }
            }
        }
        storeOnboardingScreen(onBack = back, onRolesChanged = rolesChanged)
        operatorLicenseScreen(onBack = back, onRolesChanged = rolesChanged)
        territoryScreens(onBack = back)
        partnerReviewScreens(onBack = back)
    }
}

private fun NavHostController.returnHomeWithNewRoles() {
    runCatching { getBackStackEntry<BusinessHomeDestination>() }.getOrNull()?.savedStateHandle?.set(ROLES_CHANGED, true)
    if (!popBackStack<BusinessHomeDestination>(inclusive = false)) resetTo(BusinessHomeDestination)
}
