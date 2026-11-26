package ph.foxdrop.core.network.auth

import io.ktor.client.call.body
import io.ktor.client.request.get
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.Serializable
import ph.foxdrop.core.common.result.DomainError
import ph.foxdrop.core.common.result.FoxResult
import ph.foxdrop.core.model.RoleGrant
import ph.foxdrop.core.network.FoxApiClient
import ph.foxdrop.core.network.session.ApiUser
import ph.foxdrop.core.network.session.SessionStore
import ph.foxdrop.core.network.session.toGrants

interface RolesRemoteDataSource {
    suspend fun myRoles(): FoxResult<List<RoleGrant>>
}

@Serializable
internal data class MeResponse(val data: ApiUser)

/**
 * Roles come from `/me` whenever the server is reachable, so a granted or revoked role shows on the next launch.
 * Offline (no signal in the province), the roles saved at the last sign-in or refresh pick the home; the server still
 * checks every request.
 */
@Singleton
internal class ApiRolesRemoteDataSource @Inject constructor(
    private val api: FoxApiClient,
    private val sessionStore: SessionStore,
) : RolesRemoteDataSource {
    override suspend fun myRoles(): FoxResult<List<RoleGrant>> {
        if (sessionStore.load() == null) return FoxResult.Failure(DomainError.Unauthorized)
        return when (val me = api.call { http -> http.get("me").body<MeResponse>().data }) {
            is FoxResult.Success -> {
                sessionStore.updateUser(me.value)
                FoxResult.Success(me.value.roles.toGrants())
            }

            is FoxResult.Failure -> {
                val cached = sessionStore.load()
                if (me.error == DomainError.Network &&
                    cached != null
                ) {
                    FoxResult.Success(cached.user.roles.toGrants())
                } else {
                    me
                }
            }
        }
    }
}
