import com.diffplug.gradle.spotless.SpotlessExtension
import io.gitlab.arturbosch.detekt.Detekt
import io.gitlab.arturbosch.detekt.extensions.DetektExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.withType
import ph.foxdrop.buildlogic.FOXDROP_JAVA_VERSION
import ph.foxdrop.buildlogic.lib
import ph.foxdrop.buildlogic.libs
import ph.foxdrop.buildlogic.version

/** Build output and other ecosystems' dependency folders (admin-web arrives in Phase W1). */
private val IGNORED = arrayOf("**/build/**", "**/node_modules/**", "**/vendor/**", "**/.venv/**", "admin-web/**")

/**
 * Applied once on the root project: Spotless (ktlint) and detekt (+ Compose rules) scan every module's sources,
 * and the root `check` depends on them. Running them from the root keeps them independent of the Android
 * Gradle Plugin's variant API.
 */
class QualityConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        require(target == target.rootProject) { "foxdrop.quality must be applied to the root project" }
        with(target) {
            pluginManager.apply("base")
            pluginManager.apply("com.diffplug.spotless")
            pluginManager.apply("io.gitlab.arturbosch.detekt")

            val ktlintVersion = libs.version("ktlint")
            val detektKotlin = libs.version("detektKotlin")
            extensions.configure<SpotlessExtension> {
                kotlin {
                    target("**/src/**/*.kt")
                    targetExclude(*IGNORED)
                    ktlint(ktlintVersion)
                }
                kotlinGradle {
                    target("**/*.gradle.kts")
                    targetExclude(*IGNORED)
                    ktlint(ktlintVersion)
                }
                format("misc") {
                    target("**/*.md", "**/.gitignore", "**/*.toml", "**/*.yml")
                    targetExclude(*IGNORED)
                    trimTrailingWhitespace()
                    endWithNewline()
                }
            }

            extensions.configure<DetektExtension> {
                buildUponDefaultConfig = true
                parallel = true
                config.setFrom(layout.projectDirectory.file("config/detekt/detekt.yml"))
                source.setFrom(
                    fileTree(layout.projectDirectory) {
                        include("**/src/main/**/*.kt", "**/src/test/**/*.kt", "**/src/androidTest/**/*.kt")
                        exclude(*IGNORED, "build-logic/**")
                    },
                )
            }
            tasks.withType<Detekt>().configureEach {
                jvmTarget = FOXDROP_JAVA_VERSION.toString()
                reports {
                    html.required.set(true)
                    sarif.required.set(true)
                    xml.required.set(false)
                    txt.required.set(false)
                    md.required.set(false)
                }
            }
            // detekt 1.23.8 embeds the Kotlin 2.0.21 compiler; keep its own classpath on that version
            // so it does not pick up the project's Kotlin version.
            configurations.matching { it.name == "detekt" }.configureEach {
                resolutionStrategy.eachDependency {
                    if (requested.group == "org.jetbrains.kotlin") useVersion(detektKotlin)
                }
            }
            dependencies {
                add("detektPlugins", libs.lib("compose-rules-detekt"))
            }

            tasks.named("check") {
                dependsOn("spotlessCheck", "detekt", "koverVerify")
            }
        }
    }
}
