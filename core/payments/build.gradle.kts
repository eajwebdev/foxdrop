plugins {
    alias(libs.plugins.foxdrop.android.library)
}

android {
    namespace = "ph.foxdrop.core.payments"
}

dependencies {
    implementation(libs.androidx.browser)
    implementation(libs.androidx.core.ktx)
}
