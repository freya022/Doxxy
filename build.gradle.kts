import nl.littlerobots.vcu.plugin.resolver.VersionSelectors

plugins {
    alias(libs.plugins.version.catalog.update)
}

// Repositories are required by the version catalog update plugin
repositories {
    mavenCentral()
    maven("https://jitpack.io")
}

versionCatalogUpdate {
    versionSelector(VersionSelectors.PREFER_STABLE)
}
