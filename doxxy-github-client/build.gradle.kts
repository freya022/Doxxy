plugins {
    id("doxxy-conventions")

    alias(libs.plugins.kotlinx.serialization)
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)

    implementation(libs.bundles.slf4j)

    implementation(libs.kotlinx.serialization.json)

    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.content.negotiation)

    implementation(libs.ktor.serialization.kotlinx.json)
}
