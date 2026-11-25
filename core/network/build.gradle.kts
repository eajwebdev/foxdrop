import java.util.Properties

plugins {
    alias(libs.plugins.foxdrop.android.library)
    alias(libs.plugins.foxdrop.hilt)
    alias(libs.plugins.kotlin.serialization)
}

// Public client keys come from local.properties (never committed) or CI environment variables.
// Only the Supabase anon key belongs in the app (§0 rule 9, §17).
val localProperties =
    Properties().apply {
        val file = rootProject.file("local.properties")
        if (file.exists()) file.inputStream().use { load(it) }
    }

fun clientConfig(key: String, default: String = ""): String =
    (localProperties.getProperty(key) ?: providers.environmentVariable(key).orNull ?: default)
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")

android {
    namespace = "ph.foxdrop.core.network"
    buildFeatures.buildConfig = true
    defaultConfig {
        buildConfigField("String", "SUPABASE_URL", "\"${clientConfig("SUPABASE_URL")}\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"${clientConfig("SUPABASE_ANON_KEY")}\"")
    }
}

dependencies {
    implementation(projects.core.common)
    implementation(projects.core.domain)
    implementation(projects.core.datastore)
    api(platform(libs.supabase.bom))
    api(libs.supabase.auth)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.kotlinx.serialization.json)
}
