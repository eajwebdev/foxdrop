package ph.foxdrop.core.network

/**
 * Where the FoxDrop API lives, e.g. `http://10.0.2.2:8000/api/v1/` for the local Laravel server seen from the
 * emulator. Comes from `local.properties` through BuildConfig. When it is missing the app still runs; backend calls
 * return `DomainError.NotConfigured`.
 */
data class ApiConfig(private val rawBaseUrl: String) {
    /** Always ends with `/`, so relative paths such as `auth/otp/send` resolve under it. */
    val baseUrl: String = rawBaseUrl.trim().let { if (it.isEmpty() || it.endsWith("/")) it else "$it/" }

    val isConfigured: Boolean get() = baseUrl.isNotEmpty()

    companion object {
        fun fromBuildConfig(): ApiConfig = ApiConfig(BuildConfig.FOXDROP_API_URL)
    }
}
