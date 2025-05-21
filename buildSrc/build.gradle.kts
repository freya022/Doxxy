plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    // Change in version catalog too
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:2.2.0-RC")
    implementation("com.github.ben-manes.versions:com.github.ben-manes.versions.gradle.plugin:0.52.0")
    implementation("com.google.cloud.tools:jib-gradle-plugin:3.4.4")
}
