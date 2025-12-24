pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

val includeGecko: Boolean =
    (System.getenv("INCLUDE_GECKO") ?: "false").toBoolean() ||
            (gradle.startParameter.taskNames.any {
                it.contains(
                    "GeckoIncluded",
                    ignoreCase = true
                )
            })

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven(url = "https://jitpack.io")
        if (includeGecko) {
            maven(url = "https://maven.mozilla.org/maven2/")
        }
    }
}

rootProject.name = "browkorftv"
include(":app")
include(":app:common")
include(":app:gecko")