plugins {
    alias(libs.plugins.foxdrop.android.library)
    alias(libs.plugins.foxdrop.hilt)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "ph.foxdrop.core.datastore"
}

dependencies {
    api(projects.core.model)
    implementation(projects.core.common)
    implementation(libs.androidx.datastore)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.tink.android)

    testImplementation(projects.core.testing)
}
