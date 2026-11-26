plugins {
    alias(libs.plugins.foxdrop.android.feature)
}

android {
    namespace = "ph.foxdrop.feature.rideronboarding"
}

dependencies {
    implementation(projects.core.media)
}
