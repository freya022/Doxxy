rootProject.name = "doxxy"

@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
    repositories {
        mavenCentral()

        exclusiveContent {
            forRepository {
                maven("https://jitpack.io")
            }

            filter {
                includeModule("com.github.freya022", "remark-java")
            }
        }
    }
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

include(":doxxy-commons")
include(":doxxy-docs")
include(":doxxy-bot")
include(":doxxy-backend")
include(":doxxy-github-client")
