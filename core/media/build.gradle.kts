plugins {
    alias(libs.plugins.foxdrop.android.library)
    alias(libs.plugins.foxdrop.android.compose)
    alias(libs.plugins.foxdrop.hilt)
    alias(libs.plugins.foxdrop.roborazzi)
}

android {
    namespace = "ph.foxdrop.core.media"
}

dependencies {
    api(projects.core.domain)
    implementation(projects.core.common)
    implementation(projects.core.designsystem)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.exifinterface)

    testImplementation(projects.core.testing)
}
