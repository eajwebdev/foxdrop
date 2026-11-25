package ph.foxdrop.buildlogic

import org.gradle.api.Project
import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.getByType

val Project.libs: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

fun VersionCatalog.lib(alias: String): Provider<MinimalExternalModuleDependency> = findLibrary(alias).get()

fun VersionCatalog.versionInt(alias: String): Int = findVersion(alias).get().requiredVersion.toInt()

fun VersionCatalog.version(alias: String): String = findVersion(alias).get().requiredVersion

/**
 * A public client setting (never a secret) from the root `local.properties` (not committed) or, on CI, an environment
 * variable — escaped for use inside a `buildConfigField` string literal. Empty when not set.
 */
fun Project.clientConfig(key: String): String {
    val file = rootProject.file("local.properties")
    val properties = java.util.Properties()
    if (file.exists()) file.inputStream().use(properties::load)
    val value = properties.getProperty(key) ?: providers.environmentVariable(key).orNull ?: ""
    return value.replace("\\", "\\\\").replace("\"", "\\\"")
}
