plugins {
    alias(libs.plugins.foxdrop.android.library)
    alias(libs.plugins.foxdrop.hilt)
    id("org.jetbrains.kotlinx.kover") // the root merges this module's coverage
}

android {
    namespace = "ph.foxdrop.core.data"
}

dependencies {
    api(projects.core.domain)
    implementation(projects.core.common)
    implementation(projects.core.datastore)
    implementation(projects.core.network)

    testImplementation(projects.core.testing)
}
