package ph.foxdrop.core.network.auth

import io.ktor.client.call.body
import io.ktor.client.request.get
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.Serializable
import ph.foxdrop.core.common.result.DomainError
import ph.foxdrop.core.common.result.FoxResult
import ph.foxdrop.core.common.result.map
import ph.foxdrop.core.model.RoleGrant
import ph.foxdrop.core.network.FoxApiClient
import ph.foxdrop.core.network.session.ApiUser
import ph.foxdrop.core.network.session.SessionStore
import ph.foxdrop.core.network.session.toGrants

interface RolesRemoteDataSource {
    /** The roles saved with the session at sign-in or the last refresh; null when signed out. */
    suspend fun savedRoles(): List<RoleGrant>?

    /** The server's current roles (`/me`); they replace the saved copy. */
    suspend fun fetchRoles(): FoxResult<List<RoleGrant>>
}

@Serializable
internal data class MeResponse(val data: ApiUser)

@Singleton
internal class ApiRolesRemoteDataSource @Inject constructor(
    private val api: FoxApiClient,
    private val sessionStore: SessionStore,
) : RolesRemoteDataSource {
    override suspend fun savedRoles(): List<RoleGrant>? = sessionStore.load()?.user?.roles?.toGrants()

    override suspend fun fetchRoles(): FoxResult<List<RoleGrant>> {
        val session = sessionStore.load() ?: return FoxResult.Failure(DomainError.Unauthorized)
        return api.call { http -> http.get("me").body<MeResponse>().data }.map { user ->
            sessionStore.updateUser(session.token, user)
            user.roles.toGrants()
        }
    }
}
