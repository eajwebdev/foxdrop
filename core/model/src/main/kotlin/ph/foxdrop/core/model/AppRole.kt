package ph.foxdrop.core.model

/** Mirrors the `app_role` Postgres enum (§5). [dbValue] is the exact enum label in the database. */
enum class AppRole(val dbValue: String) {
    Customer("customer"),
    Rider("rider"),
    StoreOwner("store_owner"),
    StoreStaff("store_staff"),
    Operator("operator"),
    OperatorStaff("operator_staff"),
    SuperAdmin("super_admin"),
    AdminStaff("admin_staff"),
    ;

    companion object {
        fun fromDbValue(value: String): AppRole? = entries.firstOrNull { it.dbValue == value }
    }
}

/** One role the user holds, optionally scoped to a province or a store (`user_roles` row, §5). */
data class RoleGrant(
    val role: AppRole,
    val provinceId: String? = null,
    val storeId: String? = null,
)

/** Which of the three Android apps is running. Super Admin uses the web dashboard only (§2). */
enum class FoxDropApp(val allowedRoles: Set<AppRole>) {
    Customer(setOf(AppRole.Customer)),
    Rider(setOf(AppRole.Rider)),
    Business(setOf(AppRole.StoreOwner, AppRole.StoreStaff, AppRole.Operator, AppRole.OperatorStaff)),
}
