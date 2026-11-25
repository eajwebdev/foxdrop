import androidx.room.gradle.RoomExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import ph.foxdrop.buildlogic.lib
import ph.foxdrop.buildlogic.libs

/** Room with exported schemas (committed under `schemas/`) so migrations can be tested. Used from Phase 1. */
class RoomConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("androidx.room")
            pluginManager.apply("com.google.devtools.ksp")
            extensions.configure<RoomExtension> {
                schemaDirectory("$projectDir/schemas")
            }
            dependencies {
                add("implementation", libs.lib("room-runtime"))
                add("implementation", libs.lib("room-ktx"))
                add("ksp", libs.lib("room-compiler"))
            }
        }
    }
}
