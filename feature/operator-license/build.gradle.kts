plugins {
    alias(libs.plugins.foxdrop.android.feature)
}

android {
    namespace = "ph.foxdrop.feature.operatorlicense"
}

dependencies {
    implementation(projects.core.media)
    implementation(projects.core.payments)
}
