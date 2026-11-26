package ph.foxdrop.core.data.auth

import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import ph.foxdrop.core.common.result.DomainError
import ph.foxdrop.core.common.result.FoxResult
import ph.foxdrop.core.domain.auth.RoleRepository
import ph.foxdrop.core.model.RoleGrant
import ph.foxdrop.core.network.auth.RolesRemoteDataSource

/**
 * Saved roles first, so the app opens straight to its home even with no signal in the province; then `/me`, so a
 * granted or revoked role shows without signing in again. The server authorises every request regardless.
 */
internal class DefaultRoleRepository @Inject constructor(
    private val remote: RolesRemoteDataSource,
) : RoleRepository {
    override fun roles(): Flow<FoxResult<List<RoleGrant>>> =
        flow {
            val saved = remote.savedRoles()
            if (saved != null) emit(FoxResult.Success(saved))
            when (val fresh = remote.fetchRoles()) {
                is FoxResult.Success -> if (fresh.value != saved) emit(fresh)
                is FoxResult.Failure -> if (saved == null || fresh.error != DomainError.Network) emit(fresh)
            }
        }
}
