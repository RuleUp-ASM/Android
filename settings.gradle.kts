pluginManagement {
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
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://devrepo.kakao.com/nexus/content/groups/public/") }
    }
}

rootProject.name = "Android-RuleUp"
include(":app")
include(":onboarding:data")
include(":onboarding:domain")
include(":onboarding:presentation")
include(":core:network")
include(":core:domain")
include(":core:datastore")
include(":core:designsystem")
include(":core:ui")
include(":core:map")
include(":challenge:data")
include(":challenge:domain")
include(":profile:data")
include(":profile:domain")
include(":profile:presentation")
include(":challenge:presentation")
include(":home:presentation")
include(":verification:data")
include(":verification:domain")
include(":verification:presentation")
include(":observability:domain")
include(":observability:data")
include(":observability:debug")
