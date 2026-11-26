plugins {
    alias(libs.plugins.foxdrop.android.feature)
}

android {
    namespace = "ph.foxdrop.feature.operatorzonesrates"
}

dependencies {
    implementation(projects.core.location)
}
