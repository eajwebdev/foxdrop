package ph.foxdrop.core.domain.auth

import kotlinx.coroutines.flow.Flow
import ph.foxdrop.core.common.result.FoxResult
import ph.foxdrop.core.model.RoleGrant

/**
 * The signed-in user's roles, to pick a home (§5 role routing). The server authorises every request on its own.
 */
interface RoleRepository {
    /**
     * The roles saved on this phone first (instant, works offline), then the server's when they differ or when the
     * server rejects the session. A network failure after a saved copy is not reported: the saved copy stands.
     */
    fun roles(): Flow<FoxResult<List<RoleGrant>>>
}
