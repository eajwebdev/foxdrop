import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    `kotlin-dsl`
}

group = "ph.foxdrop.buildlogic"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

dependencies {
    compileOnly(libs.android.gradle.plugin)
    compileOnly(libs.kotlin.gradle.plugin)
    compileOnly(libs.compose.compiler.gradle.plugin)
    compileOnly(libs.ksp.gradle.plugin)
    compileOnly(libs.hilt.gradle.plugin)
    compileOnly(libs.room.gradle.plugin)
    compileOnly(libs.spotless.gradle.plugin)
    compileOnly(libs.detekt.gradle.plugin)
    compileOnly(libs.kover.gradle.plugin)
    compileOnly(libs.roborazzi.gradle.plugin)
}

tasks {
    validatePlugins {
        enableStricterValidation = true
        failOnWarning = true
    }
}

gradlePlugin {
    plugins {
        register("androidApplication") {
            id = "foxdrop.android.application"
            implementationClass = "AndroidApplicationConventionPlugin"
        }
        register("androidLibrary") {
            id = "foxdrop.android.library"
            implementationClass = "AndroidLibraryConventionPlugin"
        }
        register("androidFeature") {
            id = "foxdrop.android.feature"
            implementationClass = "AndroidFeatureConventionPlugin"
        }
        register("androidCompose") {
            id = "foxdrop.android.compose"
            implementationClass = "AndroidComposeConventionPlugin"
        }
        register("hilt") {
            id = "foxdrop.hilt"
            implementationClass = "HiltConventionPlugin"
        }
        register("room") {
            id = "foxdrop.room"
            implementationClass = "RoomConventionPlugin"
        }
        register("jvmLibrary") {
            id = "foxdrop.jvm.library"
            implementationClass = "JvmLibraryConventionPlugin"
        }
        register("roborazzi") {
            id = "foxdrop.roborazzi"
            implementationClass = "RoborazziConventionPlugin"
        }
        register("quality") {
            id = "foxdrop.quality"
            implementationClass = "QualityConventionPlugin"
        }
    }
}
