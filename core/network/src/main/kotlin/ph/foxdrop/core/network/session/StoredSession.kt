package ph.foxdrop.core.network.session

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import ph.foxdrop.core.model.AppRole
import ph.foxdrop.core.model.RoleGrant

/** One role grant as the API returns it (`/me`, sign-in responses). */
@Serializable
data class UserRoleRow(
    val role: String,
    @SerialName("province_id") val provinceId: String? = null,
    @SerialName("store_id") val storeId: String? = null,
)

/** Maps rows to grants; unknown role labels (a newer server) are ignored rather than crashing old apps. */
fun List<UserRoleRow>.toGrants(): List<RoleGrant> =
    mapNotNull { row -> AppRole.fromDbValue(row.role)?.let { RoleGrant(it, row.provinceId, row.storeId) } }

/** The signed-in user as `UserResource` returns it. */
@Serializable
data class ApiUser(
    val id: String,
    @SerialName("full_name") val fullName: String? = null,
    val phone: String? = null,
    val email: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    val roles: List<UserRoleRow> = emptyList(),
)

/** What this phone keeps after sign-in: the bearer token and the last known user (for offline role routing). */
@Serializable
data class StoredSession(
    val token: String,
    @SerialName("expires_at") val expiresAt: String? = null,
    val user: ApiUser,
)
