package ph.foxdrop.core.domain.startup

import javax.inject.Inject
import ph.foxdrop.core.common.result.FoxResult
import ph.foxdrop.core.common.result.map
import ph.foxdrop.core.domain.auth.RoleRepository
import ph.foxdrop.core.model.AppRole
import ph.foxdrop.core.model.FoxDropApp
import ph.foxdrop.core.model.RoleGrant

/** Which home a signed-in user lands on in a given app (§2, §5 role routing). */
sealed interface HomeDecision {
    /** A home this app can show. */
    sealed interface Home : HomeDecision

    /** The account is valid but does not belong in this app: the app explains where to go and offers sign out. */
    sealed interface Mismatch : HomeDecision

    data object CustomerHome : Home

    data class RiderHome(val provinceId: String) : Home

    data class StoreHome(val storeId: String) : Home

    data class OperatorHome(val provinceId: String) : Home

    /** Signed in to the rider app without a rider account yet (registration arrives in Phase 4). */
    data object RiderNotRegistered : Mismatch

    /** Signed in to the business app without a store or operator role (applications arrive in Phase 2). */
    data object BusinessNotRegistered : Mismatch

    /** Super Admin and admin staff use the web dashboard only (§2). */
    data object AdminUseWeb : Mismatch
}

class ResolveHomeUseCase @Inject constructor(
    private val roles: RoleRepository,
) {
    suspend operator fun invoke(app: FoxDropApp): FoxResult<HomeDecision> = roles.myRoles().map { decideHome(app, it) }

    companion object {
        /**
         * Pure routing rules. Admin accounts are kept out of every Android app; in the business app an operator role
         * wins over a store role (an operator who also owns a store manages the province first). Grants arrive oldest
         * first, so a multi-store owner always lands on the same store.
         */
        fun decideHome(app: FoxDropApp, grants: List<RoleGrant>): HomeDecision {
            if (grants.any { it.role in AppRole.ADMIN }) return HomeDecision.AdminUseWeb
            return when (app) {
                FoxDropApp.Customer -> HomeDecision.CustomerHome

                FoxDropApp.Rider ->
                    grants.scope(setOf(AppRole.Rider)) { it.provinceId }?.let(HomeDecision::RiderHome)
                        ?: HomeDecision.RiderNotRegistered

                FoxDropApp.Business ->
                    grants.scope(AppRole.OPERATOR) { it.provinceId }?.let(HomeDecision::OperatorHome)
                        ?: grants.scope(AppRole.STORE) { it.storeId }?.let(HomeDecision::StoreHome)
                        ?: HomeDecision.BusinessNotRegistered
            }
        }

        /** The scope (province or store id) of the first grant with one of [roles]. */
        private fun List<RoleGrant>.scope(roles: Set<AppRole>, scopeOf: (RoleGrant) -> String?): String? =
            firstNotNullOfOrNull { grant -> scopeOf(grant)?.takeIf { grant.role in roles } }
    }
}
