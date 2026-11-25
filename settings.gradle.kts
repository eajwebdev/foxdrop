pluginManagement {
    includeBuild("build-logic")
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
    }
}

rootProject.name = "FoxDrop"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

include(":app-customer")
include(":app-rider")
include(":app-business")

include(":core:model")
include(":core:common")
include(":core:domain")
include(":core:datastore")
include(":core:network")
include(":core:data")
include(":core:designsystem")
include(":core:testing")

include(":feature:onboarding")
include(":feature:auth")
