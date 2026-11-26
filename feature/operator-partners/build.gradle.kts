plugins {
    alias(libs.plugins.foxdrop.android.feature)
}

android {
    namespace = "ph.foxdrop.feature.operatorpartners"
}

dependencies {
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
}
