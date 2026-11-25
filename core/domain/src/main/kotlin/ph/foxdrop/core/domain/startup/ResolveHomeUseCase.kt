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
    data object CustomerHome : HomeDecision

    data class RiderHome(val provinceId: String) : HomeDecision

    /** Signed in to the rider app without a rider account yet (registration arrives in Phase 4). */
    data object RiderNotRegistered : HomeDecision

    data class StoreHome(val storeId: String) : HomeDecision

    data class OperatorHome(val provinceId: String) : HomeDecision

    /** Signed in to the business app without a store or operator role (applications arrive in Phase 2). */
    data object BusinessNotRegistered : HomeDecision

    /** Super Admin and admin staff use the web dashboard only (§2). */
    data object AdminUseWeb : HomeDecision
}

class ResolveHomeUseCase @Inject constructor(
    private val roles: RoleRepository,
) {
    suspend operator fun invoke(app: FoxDropApp): FoxResult<HomeDecision> = roles.myRoles().map { decideHome(app, it) }

    companion object {
        private val ADMIN_ROLES = setOf(AppRole.SuperAdmin, AppRole.AdminStaff)
        private val OPERATOR_ROLES = setOf(AppRole.Operator, AppRole.OperatorStaff)
        private val STORE_ROLES = setOf(AppRole.StoreOwner, AppRole.StoreStaff)

        /**
         * Pure routing rules. Admin accounts are kept out of every Android app; in the business app an operator role
         * wins over a store role (an operator who also owns a store manages the province first).
         */
        fun decideHome(app: FoxDropApp, grants: List<RoleGrant>): HomeDecision {
            if (grants.any { it.role in ADMIN_ROLES }) return HomeDecision.AdminUseWeb
            return when (app) {
                FoxDropApp.Customer -> HomeDecision.CustomerHome

                FoxDropApp.Rider ->
                    grants.firstNotNullOfOrNull { grant -> grant.provinceId?.takeIf { grant.role == AppRole.Rider } }
                        ?.let(HomeDecision::RiderHome)
                        ?: HomeDecision.RiderNotRegistered

                FoxDropApp.Business -> businessHome(grants)
            }
        }

        private fun businessHome(grants: List<RoleGrant>): HomeDecision {
            grants.firstNotNullOfOrNull { grant -> grant.provinceId?.takeIf { grant.role in OPERATOR_ROLES } }
                ?.let { return HomeDecision.OperatorHome(it) }
            grants.firstNotNullOfOrNull { grant -> grant.storeId?.takeIf { grant.role in STORE_ROLES } }
                ?.let { return HomeDecision.StoreHome(it) }
            return HomeDecision.BusinessNotRegistered
        }
    }
}
