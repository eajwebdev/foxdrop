package ph.foxdrop.core.network

/**
 * Public client settings for Supabase. Values come from `local.properties` through BuildConfig.
 * When they are missing the app still runs; backend calls return `DomainError.NotConfigured`.
 */
data class SupabaseConfig(val url: String, val anonKey: String) {
    val isConfigured: Boolean get() = url.isNotBlank() && anonKey.isNotBlank()

    override fun toString(): String {
        val key = if (anonKey.isBlank()) "<empty>" else "<set>"
        return "SupabaseConfig(url=$url, anonKey=$key)"
    }

    companion object {
        fun fromBuildConfig(): SupabaseConfig = SupabaseConfig(BuildConfig.SUPABASE_URL, BuildConfig.SUPABASE_ANON_KEY)
    }
}
