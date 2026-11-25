plugins {
    alias(libs.plugins.foxdrop.android.application)
}

// Shared app dependencies come from the foxdrop.android.application convention plugin.
android {
    namespace = "ph.foxdrop.customer"
    defaultConfig {
        applicationId = "ph.foxdrop.customer"
        versionCode = 1
        versionName = "0.1.0"
    }
}
