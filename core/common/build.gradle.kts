plugins {
    alias(libs.plugins.foxdrop.jvm.library)
}

dependencies {
    api(projects.core.model)
    api(libs.kotlinx.coroutines.core)
    implementation(libs.javax.inject)
}
