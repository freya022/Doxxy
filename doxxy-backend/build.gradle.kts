plugins {
    id("doxxy-conventions")
    id("doxxy-app-conventions")

    alias(libs.plugins.spring)
    alias(libs.plugins.jpa)
    alias(libs.plugins.spring.boot)
}

group = "io.github.freya022"

dependencies {
    implementation(projects.doxxy.doxxyCommons)

    implementation(libs.postgresql)
    implementation(libs.bundles.flyway)

    implementation(libs.kotlinx.serialization.json)

    implementation(platform(org.springframework.boot.gradle.plugin.SpringBootPlugin.BOM_COORDINATES))
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-web")
}
