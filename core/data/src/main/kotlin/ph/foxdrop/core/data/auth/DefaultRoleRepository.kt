package ph.foxdrop.core.data.auth

import javax.inject.Inject
import ph.foxdrop.core.common.result.FoxResult
import ph.foxdrop.core.domain.auth.RoleRepository
import ph.foxdrop.core.model.RoleGrant
import ph.foxdrop.core.network.auth.RolesRemoteDataSource

/**
 * Roles always come from the server: they decide which home and data a user gets, so a stale cached copy must never
 * outlive a suspension. (Offline caching of the last decision can come with Room in Phase 3.)
 */
internal class DefaultRoleRepository @Inject constructor(
    private val remote: RolesRemoteDataSource,
) : RoleRepository {
    override suspend fun myRoles(): FoxResult<List<RoleGrant>> = remote.myRoles()
}
