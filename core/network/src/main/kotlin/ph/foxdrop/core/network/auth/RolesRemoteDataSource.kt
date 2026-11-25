package ph.foxdrop.core.network.auth

import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
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

@Singleton
internal class SupabaseRolesRemoteDataSource @Inject constructor(
    private val provider: SupabaseClientProvider,
) : RolesRemoteDataSource {
    override suspend fun myRoles(): FoxResult<List<RoleGrant>> {
        val result =
            provider.call { client ->
                val userId = client.auth.currentUserOrNull()?.id ?: return@call null
                // RLS returns more than our own rows to operators (their province's grants), so filter by user.
                client.from("user_roles")
                    .select(Columns.list("role", "province_id", "store_id")) {
                        filter { eq("user_id", userId) }
                        order("created_at", Order.ASCENDING) // oldest grant first: a stable home for multi-store owners
                    }
                    .decodeList<UserRoleRow>()
                    .toGrants()
            }
        return when (result) {
            is FoxResult.Success -> result.value?.let { FoxResult.Success(it) }
                ?: FoxResult.Failure(DomainError.Unauthorized)

            is FoxResult.Failure -> result
        }
    }
}
