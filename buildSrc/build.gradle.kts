plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    // Change in version catalog too
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:2.1.10")
    implementation("com.google.cloud.tools:jib-gradle-plugin:3.4.4")
}
