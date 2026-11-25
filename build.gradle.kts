// Root build: puts plugins on the classpath with one version each; modules apply them through build-logic.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.room) apply false
    alias(libs.plugins.spotless) apply false
    alias(libs.plugins.detekt) apply false
    alias(libs.plugins.kover)
    alias(libs.plugins.roborazzi) apply false
    id("foxdrop.quality")
}

dependencies {
    // Merged coverage report/verification for the domain and data layers (§16: Kover ≥ 80 %).
    kover(projects.core.domain)
    kover(projects.core.data)
    kover(projects.core.common)
    kover(projects.core.model)
}

kover {
    reports {
        filters {
            excludes {
                annotatedBy("dagger.Module", "javax.annotation.processing.Generated")
                classes("*_Factory", "*_Factory\$*", "*_MembersInjector", "*Hilt_*", "*.di.*", "*.BuildConfig")
            }
        }
        verify {
            rule("Domain and data line coverage") {
                minBound(80)
            }
        }
    }
}
