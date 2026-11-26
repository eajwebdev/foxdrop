plugins {
    alias(libs.plugins.foxdrop.android.library)
    alias(libs.plugins.foxdrop.android.compose)
    alias(libs.plugins.foxdrop.hilt)
}

android {
    namespace = "ph.foxdrop.core.location"
}

dependencies {
    api(projects.core.model)
    implementation(projects.core.common)
    implementation(libs.play.services.location)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)
    testImplementation(projects.core.testing)
}
