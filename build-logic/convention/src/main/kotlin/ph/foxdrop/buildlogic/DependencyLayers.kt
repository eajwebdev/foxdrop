package ph.foxdrop.buildlogic

import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.artifacts.ProjectDependency

/**
 * Module layers (MASTER_PROMPT.md §16): apps wire features together; features use only core; core uses only core.
 * A higher layer may depend on a lower one, never the reverse, and features never depend on each other.
 */
private enum class Layer { Core, Feature, App }

private fun layerOf(path: String): Layer? =
    when {
        path.startsWith(":core:") -> Layer.Core
        path.startsWith(":feature:") -> Layer.Feature
        path.startsWith(":app-") -> Layer.App
        else -> null
    }

/** Fails the build as soon as a forbidden project dependency is declared. Applied by every module convention. */
internal fun Project.enforceDependencyLayers() {
    val self = path
    val selfLayer = layerOf(self) ?: return
    configurations.configureEach {
        val configurationName = name
        dependencies.configureEach {
            if (this !is ProjectDependency || path == self) return@configureEach
            val targetLayer = layerOf(path) ?: return@configureEach
            val allowed =
                targetLayer < selfLayer || (targetLayer == selfLayer && selfLayer == Layer.Core)
            if (!allowed) {
                throw GradleException(
                    "Dependency rule broken: $self -> $path ($configurationName). " +
                        "Apps may use features and core, features only core, core only core " +
                        "(MASTER_PROMPT.md §16). Wire features together in the app module instead.",
                )
            }
        }
    }
}
