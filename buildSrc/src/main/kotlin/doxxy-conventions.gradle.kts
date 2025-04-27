import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm")

    id("com.github.ben-manes.versions")
}

version = "2.5"

repositories {
    mavenCentral()
    maven("https://jitpack.io")
}

private val matchers: Map<String, Regex> = mapOf(
    "org.junit.jupiter" to Regex(".+-M\\d+"),
    "net.dv8tion" to Regex(".+_DEV"),
    "com.fasterxml.jackson" to Regex(".+-rc.+"),
    "org.slf4j" to Regex(".+-alpha.+"),
    "io.ktor" to Regex(".+-beta.+"),
    "org.jetbrains.kotlin" to Regex(".+-(?:Beta|RC).*"),
    "org.jetbrains.kotlinx" to Regex(".+-(?:Beta|RC).*"),
)

tasks.withType<DependencyUpdatesTask> {
    rejectVersionIf {
        matchers.any { (packagePrefix, regex) -> packagePrefix in candidate.group && candidate.version.matches(regex) }
    }
}

dependencies {
    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.10.0")

    implementation("io.github.oshai:kotlin-logging-jvm:7.0.0")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(24)
    }

    sourceCompatibility = JavaVersion.VERSION_22
    targetCompatibility = JavaVersion.VERSION_22
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_22

        freeCompilerArgs.addAll("-Xjsr305=strict", "-Xcontext-receivers", "-Xsuppress-warning=CONTEXT_RECEIVERS_DEPRECATED")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}
