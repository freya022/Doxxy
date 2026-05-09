import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm")
}

group = "dev.freya02"
version = "2.5"

repositories {
    mavenCentral()
}

configurations.all {
    exclude(module = "opus-java")
    exclude(module = "tink")
}

dependencies {
    implementation("io.github.oshai:kotlin-logging-jvm:7.0.0")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict", "-Xcontext-parameters")
    }
}
