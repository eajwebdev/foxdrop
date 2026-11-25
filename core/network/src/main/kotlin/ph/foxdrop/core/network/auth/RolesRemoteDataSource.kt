package ph.foxdrop.core.network.auth

import io.github.jan.supabase.auth.auth
import java.util.Base64
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import ph.foxdrop.core.common.result.DomainError
import ph.foxdrop.core.common.result.FoxResult
import ph.foxdrop.core.model.AppRole
import ph.foxdrop.core.model.RoleGrant
import ph.foxdrop.core.network.SupabaseClientProvider

interface RolesRemoteDataSource {
    suspend fun myRoles(): FoxResult<List<RoleGrant>>
}

@Serializable
internal data class UserRoleRow(
    val role: String,
    @SerialName("province_id") val provinceId: String? = null,
    @SerialName("store_id") val storeId: String? = null,
)

/** Maps rows to grants; unknown role labels (a newer server) are ignored rather than crashing old apps. */
internal fun List<UserRoleRow>.toGrants(): List<RoleGrant> =
    mapNotNull { row -> AppRole.fromDbValue(row.role)?.let { RoleGrant(it, row.provinceId, row.storeId) } }

private val claimsJson = Json { ignoreUnknownKeys = true }

/**
 * Reads the `app_roles` claim that `custom_access_token_hook` puts in every access token (§5). The signature is not
 * checked: this only picks which home to show, and RLS decides every read. Returns null for a malformed token.
 */
internal fun appRolesFromAccessToken(token: String): List<RoleGrant>? =
    runCatching {
        val payload = String(Base64.getUrlDecoder().decode(token.split('.')[1]))
        val roles = claimsJson.parseToJsonElement(payload).jsonObject["app_roles"] ?: return@runCatching emptyList()
        claimsJson.decodeFromJsonElement<List<UserRoleRow>>(roles).toGrants()
    }.getOrNull()

/**
 * Roles come from the stored session, so routing works offline and costs no request. A role change shows after the
 * next token refresh (at most one token lifetime); flows that grant a role refresh the session themselves.
 */
@Singleton
internal class SupabaseRolesRemoteDataSource @Inject constructor(
    private val provider: SupabaseClientProvider,
) : RolesRemoteDataSource {
    override suspend fun myRoles(): FoxResult<List<RoleGrant>> =
        when (val token = provider.call { it.auth.currentAccessTokenOrNull() }) {
            is FoxResult.Failure -> token

            is FoxResult.Success ->
                token.value?.let(::appRolesFromAccessToken)?.let { FoxResult.Success(it) }
                    ?: FoxResult.Failure(DomainError.Unauthorized)
        }
}
