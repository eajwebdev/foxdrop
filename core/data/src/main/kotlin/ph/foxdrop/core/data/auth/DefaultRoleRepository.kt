package ph.foxdrop.core.data.auth

import javax.inject.Inject
import ph.foxdrop.core.common.result.FoxResult
import ph.foxdrop.core.domain.auth.RoleRepository
import ph.foxdrop.core.model.RoleGrant
import ph.foxdrop.core.network.auth.RolesRemoteDataSource

/**
 * Roles from the server when reachable, else the copy saved at the last sign-in (offline routing). They only pick a
 * home; the server authorises every request.
 */
internal class DefaultRoleRepository @Inject constructor(
    private val remote: RolesRemoteDataSource,
) : RoleRepository {
    override suspend fun myRoles(): FoxResult<List<RoleGrant>> = remote.myRoles()
}
