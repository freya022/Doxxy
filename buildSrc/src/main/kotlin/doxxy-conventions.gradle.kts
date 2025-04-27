import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm")
}

//group = "com.freya02"
version = "2.5"

repositories {
    mavenCentral()
    maven("https://jitpack.io")
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
