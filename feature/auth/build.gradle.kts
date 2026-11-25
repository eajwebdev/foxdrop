import ph.foxdrop.buildlogic.clientConfig

plugins {
    alias(libs.plugins.foxdrop.android.feature)
}

android {
    namespace = "ph.foxdrop.feature.auth"
    buildFeatures.buildConfig = true
    defaultConfig {
        // OAuth "Web application" client ID from Google Cloud (public). Empty = Google sign-in shows "coming soon".
        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"${clientConfig("GOOGLE_WEB_CLIENT_ID")}\"")
    }
}

dependencies {
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)
    implementation(libs.play.services.auth.api.phone)
}
