package ph.foxdrop.core.domain.auth

import ph.foxdrop.core.common.result.FoxResult
import ph.foxdrop.core.model.RoleGrant

/** The signed-in user's roles, read fresh from the server (`user_roles`, protected by RLS). */
interface RoleRepository {
    suspend fun myRoles(): FoxResult<List<RoleGrant>>
}
