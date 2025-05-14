plugins {
    id("doxxy-conventions")
    id("doxxy-app-conventions")

    alias(libs.plugins.spring)
    alias(libs.plugins.jpa)
    alias(libs.plugins.spring.boot)

    alias(libs.plugins.kotlinx.serialization)
}

dependencies {
    implementation(projects.doxxy.doxxyCommons)

    implementation(libs.postgresql)
    implementation(libs.bundles.flyway)

    implementation(libs.kotlin.reflect) // Spring depends on it
    implementation(libs.kotlinx.serialization.json)

    implementation(platform(org.springframework.boot.gradle.plugin.SpringBootPlugin.BOM_COORDINATES))
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-web")
}

jib {
    from {
        image = "eclipse-temurin:24-jre"
    }

    to {
        image = "ghcr.io/freya022/doxxy-backend"
    }

    container {
        mainClass = "dev.freya02.doxxy.backend.BackendApplicationKt"
    }
}
