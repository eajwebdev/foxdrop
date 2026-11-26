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
include(":core:payments")
include(":core:media")
include(":core:location")

include(":feature:onboarding")
include(":feature:auth")
include(":feature:operator-license")
include(":feature:operator-zones-rates")
include(":feature:operator-partners")
include(":feature:store-onboarding")
include(":feature:rider-onboarding")
