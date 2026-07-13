// Top-level Gradle settings for Elendheim Spark.
// This file wires up where Gradle looks for plugins and dependencies,
// and lists the modules that make up the build.

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

dependencyResolutionManagement {
    // Fail fast if a module tries to declare its own repositories ->
    // keeps all dependency sources in one predictable place.
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Elendheim Spark"
include(":app")
