import org.gradle.api.Plugin
import org.gradle.api.Project
import ph.foxdrop.buildlogic.addUnitTestDependencies
import ph.foxdrop.buildlogic.configureJavaToolchainless
import ph.foxdrop.buildlogic.configureKotlinCompiler
import ph.foxdrop.buildlogic.enforceDependencyLayers

/**
 * Pure Kotlin module (no Android): `:core:model`, `:core:domain`, `:core:common`.
 * Kover is applied here because the root merges these modules' coverage (§16: ≥ 80 %).
 */
class JvmLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("org.jetbrains.kotlin.jvm")
            pluginManager.apply("org.jetbrains.kotlinx.kover")
            configureJavaToolchainless()
            configureKotlinCompiler()
            addUnitTestDependencies()
            enforceDependencyLayers()
        }
    }
}
