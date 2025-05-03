plugins {
    id("doxxy-conventions")
    alias(libs.plugins.kotlinx.serialization)
}

dependencies {
    implementation(libs.dotenv.kotlin)
    implementation(libs.kotlinx.serialization.json)
}
