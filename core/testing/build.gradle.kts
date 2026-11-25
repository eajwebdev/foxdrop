plugins {
    alias(libs.plugins.foxdrop.android.library)
    alias(libs.plugins.foxdrop.android.compose)
}

android {
    namespace = "ph.foxdrop.core.testing"
}

dependencies {
    api(platform(libs.androidx.compose.bom))
    api(projects.core.common)
    api(projects.core.domain)
    api(projects.core.designsystem)
    api(libs.junit4)
    api(libs.kotlinx.coroutines.test)
    api(libs.turbine)
    api(libs.robolectric)
    api(libs.roborazzi)
    api(libs.roborazzi.compose)
    api(libs.roborazzi.junit.rule)
    api(libs.androidx.compose.ui.test.junit4)
    implementation(libs.androidx.activity.compose)
    api(libs.androidx.test.core)
    api(libs.androidx.test.ext.junit)
}
