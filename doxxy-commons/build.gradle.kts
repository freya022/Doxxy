plugins {
    id("doxxy-conventions")
    alias(libs.plugins.kotlinx.serialization)
}

// TODO update to dev.freya02
group = "io.github.freya022"

dependencies {
    implementation(libs.kotlinx.serialization.json)
}
