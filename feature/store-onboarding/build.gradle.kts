plugins {
    alias(libs.plugins.foxdrop.android.feature)
}

android {
    namespace = "ph.foxdrop.feature.storeonboarding"
}

dependencies {
    implementation(projects.core.media)
    implementation(projects.core.location)
}
