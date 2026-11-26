import ph.foxdrop.buildlogic.clientConfig

plugins {
    alias(libs.plugins.foxdrop.android.library)
    alias(libs.plugins.foxdrop.hilt)
    alias(libs.plugins.kotlin.serialization)
}

// The FoxDrop API base URL comes from local.properties (never committed) or a CI environment variable. The app holds
// no server secrets: users get their own Sanctum token at sign-in (§17).
android {
    namespace = "ph.foxdrop.core.network"
    buildFeatures.buildConfig = true
    defaultConfig {
        buildConfigField("String", "FOXDROP_API_URL", "\"${clientConfig("FOXDROP_API_URL")}\"")
    }
}

dependencies {
    implementation(projects.core.common)
    implementation(projects.core.domain)
    implementation(projects.core.datastore)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.junit4)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.ktor.client.mock)
}
