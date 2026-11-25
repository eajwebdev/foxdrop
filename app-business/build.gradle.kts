plugins {
    alias(libs.plugins.foxdrop.android.application)
}

// Shared app dependencies come from the foxdrop.android.application convention plugin.
android {
    namespace = "ph.foxdrop.business"
    defaultConfig {
        applicationId = "ph.foxdrop.business"
        versionCode = 1
        versionName = "0.1.0"
    }
}
