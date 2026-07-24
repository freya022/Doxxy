plugins {
    kotlin("jvm")
}

group = "dev.freya02"
version = "2.5"

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
        freeCompilerArgs.addAll(
            "-Xname-based-destructuring=complete"
        )
    }
}
