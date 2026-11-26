plugins {
    alias(libs.plugins.foxdrop.android.library)
    alias(libs.plugins.foxdrop.android.compose)
    alias(libs.plugins.foxdrop.roborazzi)
}

android {
    namespace = "ph.foxdrop.core.designsystem"
}

dependencies {
    api(libs.androidx.compose.foundation)
    api(libs.androidx.compose.animation)
    api(libs.androidx.compose.material3)
    api(libs.androidx.compose.material.icons.extended)
    api(libs.androidx.compose.ui)
    api(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.material3.navigation.suite)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.activity.compose)
    implementation(projects.core.model)
    implementation(projects.core.common)
    implementation(projects.core.domain) // field-problem messages for forms
    implementation(libs.kotlinx.collections.immutable)

    testImplementation(projects.core.testing)
}
