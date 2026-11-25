import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.android.build.api.variant.HasHostTests
import com.android.build.api.variant.HostTestBuilder
import com.android.build.api.variant.LibraryAndroidComponentsExtension
import com.android.build.api.variant.Variant
import io.github.takahirom.roborazzi.RoborazziExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import ph.foxdrop.buildlogic.lib
import ph.foxdrop.buildlogic.libs

/**
 * Roborazzi screenshot tests on Robolectric (JVM, no emulator).
 * Golden images are committed in `src/test/screenshots`.
 * Record: `./gradlew recordRoborazziDebug` · Verify: `./gradlew verifyRoborazziDebug`.
 *
 * Every module that runs Robolectric gets `config/robolectric/robolectric.properties` as a unit-test resource
 * (SDK 36 runtime + plain Application; see docs/DECISIONS.md 4), so no test has to repeat `@Config`.
 */
class RoborazziConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("io.github.takahirom.roborazzi")
            extensions.configure<RoborazziExtension> {
                outputDir.set(layout.projectDirectory.dir("src/test/screenshots"))
            }
            val robolectricConfig = rootProject.layout.projectDirectory.dir("config/robolectric").asFile.path
            pluginManager.withPlugin("com.android.application") {
                extensions.configure<ApplicationAndroidComponentsExtension> {
                    onVariants { it.addUnitTestResources(robolectricConfig) }
                }
            }
            pluginManager.withPlugin("com.android.library") {
                extensions.configure<LibraryAndroidComponentsExtension> {
                    onVariants { it.addUnitTestResources(robolectricConfig) }
                }
            }
            dependencies {
                listOf(
                    "robolectric",
                    "roborazzi",
                    "roborazzi-compose",
                    "roborazzi-junit-rule",
                    "androidx-compose-ui-test-junit4",
                    "androidx-test-core",
                    "androidx-test-ext-junit",
                ).forEach { add("testImplementation", libs.lib(it)) }
                add("debugImplementation", libs.lib("androidx-compose-ui-test-manifest"))
            }
        }
    }
}

/** AGP 9: unit tests are the `UNIT_TEST_TYPE` host test of a variant. */
private fun Variant.addUnitTestResources(directory: String) {
    (this as? HasHostTests)
        ?.hostTests
        ?.get(HostTestBuilder.UNIT_TEST_TYPE)
        ?.sources
        ?.resources
        ?.addStaticSourceDirectory(directory)
}
