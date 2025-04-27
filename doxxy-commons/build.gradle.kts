plugins {
    id("doxxy-conventions")
    alias(libs.plugins.kotlinx.serialization)
}

group = "io.github.freya022"

dependencies {
    implementation(libs.kotlinx.serialization.json)
}
