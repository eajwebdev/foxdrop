package ph.foxdrop.buildlogic

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.Project

/** Settings shared by every Android module (apps and libraries). */
internal fun Project.configureAndroidCommon(android: CommonExtension) {
    android.apply {
        compileSdk = libs.versionInt("compileSdk")
        defaultConfig.minSdk = libs.versionInt("minSdk")
        defaultConfig.testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        compileOptions.sourceCompatibility = FOXDROP_JAVA_VERSION
        compileOptions.targetCompatibility = FOXDROP_JAVA_VERSION

        testOptions.unitTests.isIncludeAndroidResources = true

        lint.apply {
            warningsAsErrors = true
            abortOnError = true
            checkReleaseBuilds = true
            // Versions are pinned on purpose to the newest *compatible* set (docs/DECISIONS.md),
            // so "a newer version exists" is not a defect.
            disable +=
                setOf(
                    "GradleDependency",
                    "NewerVersionAvailable",
                    "AndroidGradlePluginVersion",
                    "OldTargetApi",
                )
        }
    }
    configureKotlinCompiler()
    addUnitTestDependencies()
    enforceDependencyLayers()
}
