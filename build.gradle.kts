import nl.littlerobots.vcu.plugin.resolver.VersionSelectors

plugins {
    alias(libs.plugins.version.catalog.update)
}

// Repositories are required by the version catalog update plugin
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

versionCatalogUpdate {
    versionSelector(VersionSelectors.PREFER_STABLE)
}
