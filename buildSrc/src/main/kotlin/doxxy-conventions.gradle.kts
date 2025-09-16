import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm")
}

group = "dev.freya02"
version = "2.5"

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.github.oshai:kotlin-logging-jvm:7.0.0")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(24)
    }

    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_21

        freeCompilerArgs.addAll("-Xjsr305=strict", "-Xcontext-parameters")
    }
}
