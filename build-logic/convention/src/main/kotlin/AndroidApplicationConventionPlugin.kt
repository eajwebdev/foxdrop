import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import ph.foxdrop.buildlogic.configureAndroidCommon
import ph.foxdrop.buildlogic.lib
import ph.foxdrop.buildlogic.libs
import ph.foxdrop.buildlogic.versionInt

class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.android.application")
            pluginManager.apply("foxdrop.android.compose")
            pluginManager.apply("foxdrop.hilt")
            pluginManager.apply("foxdrop.roborazzi")
            pluginManager.apply("org.jetbrains.kotlin.plugin.serialization")

            // Every app wires the same shared features and core modules; app-specific code stays in the app.
            dependencies {
                listOf(":core:designsystem", ":core:data", ":feature:onboarding", ":feature:auth").forEach {
                    add("implementation", project(it))
                }
                listOf(
                    "androidx-activity-compose",
                    "androidx-lifecycle-runtime-compose",
                    "androidx-hilt-lifecycle-viewmodel-compose",
                    "androidx-navigation-compose",
                    "androidx-profileinstaller",
                    "kotlinx-serialization-json",
                    "kotlinx-collections-immutable",
                    "timber",
                ).forEach { add("implementation", libs.lib(it)) }
                add("testImplementation", project(":core:testing"))
            }

            extensions.configure<ApplicationExtension> {
                configureAndroidCommon(this)
                defaultConfig.targetSdk = libs.versionInt("targetSdk")
                buildFeatures.buildConfig = true

                buildTypes.getByName("release").apply {
                    isMinifyEnabled = true
                    isShrinkResources = true
                    proguardFiles(
                        getDefaultProguardFile("proguard-android-optimize.txt"),
                        "proguard-rules.pro",
                    )
                }
                buildTypes.getByName("debug").apply {
                    applicationIdSuffix = ".debug"
                    versionNameSuffix = "-debug"
                }
                packaging.resources.excludes +=
                    setOf("/META-INF/{AL2.0,LGPL2.1}", "/META-INF/LICENSE*", "/META-INF/NOTICE*")
            }
        }
    }
}
