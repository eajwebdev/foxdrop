import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import ph.foxdrop.buildlogic.lib
import ph.foxdrop.buildlogic.libs

/**
 * A feature module: Compose UI + ViewModel, depends only on `:core:*` (§16 dependency rule).
 * The layer rule (ph.foxdrop.buildlogic.DependencyLayers) fails the build on a feature → feature dependency.
 */
class AndroidFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("foxdrop.android.library")
            pluginManager.apply("foxdrop.android.compose")
            pluginManager.apply("foxdrop.hilt")
            pluginManager.apply("foxdrop.roborazzi")
            pluginManager.apply("org.jetbrains.kotlin.plugin.serialization")

            dependencies {
                add("implementation", project(":core:designsystem"))
                add("implementation", project(":core:common"))
                add("implementation", project(":core:model"))
                add("implementation", project(":core:domain"))

                add("implementation", libs.lib("androidx-lifecycle-runtime-compose"))
                add("implementation", libs.lib("androidx-lifecycle-viewmodel-compose"))
                add("implementation", libs.lib("androidx-hilt-lifecycle-viewmodel-compose"))
                add("implementation", libs.lib("androidx-navigation-compose"))
                add("implementation", libs.lib("androidx-activity-compose"))
                add("implementation", libs.lib("kotlinx-serialization-json"))
                add("implementation", libs.lib("kotlinx-collections-immutable"))

                add("testImplementation", project(":core:testing"))
            }
        }
    }
}
