plugins {
    alias(libs.plugins.foxdrop.jvm.library)
}

// Pure Kotlin: no Android, no Supabase (§16).
dependencies {
    api(projects.core.model)
    api(projects.core.common)
    implementation(libs.javax.inject)
}
