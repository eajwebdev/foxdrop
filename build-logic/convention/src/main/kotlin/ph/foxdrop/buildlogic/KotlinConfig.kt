package ph.foxdrop.buildlogic

import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompilerOptions
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

/** The one Java/JVM target for every module, build-logic tool config included. */
val FOXDROP_JAVA_VERSION: JavaVersion = JavaVersion.VERSION_17

/**
 * Kotlin compiler settings shared by Android and JVM modules.
 * Warnings are errors so new problems are fixed, not ignored (§16).
 */
internal fun Project.configureKotlinCompiler() {
    val warningsAsErrors = providers.gradleProperty("foxdrop.warningsAsErrors").orNull?.toBoolean() ?: true
    tasks.withType<KotlinCompilationTask<*>>().configureEach {
        compilerOptions {
            allWarningsAsErrors.set(warningsAsErrors)
            freeCompilerArgs.add("-Xconsistent-data-class-copy-visibility")
            if (this is KotlinJvmCompilerOptions) {
                jvmTarget.set(JvmTarget.fromTarget(FOXDROP_JAVA_VERSION.toString()))
            }
        }
    }
}

internal fun Project.configureJavaToolchainless() {
    extensions.configure<JavaPluginExtension> {
        sourceCompatibility = FOXDROP_JAVA_VERSION
        targetCompatibility = FOXDROP_JAVA_VERSION
    }
}

/** JUnit 4, coroutines-test, Turbine and MockK for every module's unit tests. */
internal fun Project.addUnitTestDependencies() {
    dependencies {
        listOf("junit4", "kotlinx-coroutines-test", "turbine", "mockk").forEach {
            add("testImplementation", libs.lib(it))
        }
    }
}
