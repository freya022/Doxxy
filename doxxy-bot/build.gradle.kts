plugins {
    id("doxxy-conventions")
    id("doxxy-app-conventions")
}

group = "io.github.freya022"

dependencies {
    implementation(projects.doxxy.doxxyCommons)

    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.debug)
    implementation(libs.stacktrace.decoroutinator)

    implementation(libs.bundles.slf4j)
    implementation(libs.logback.classic)

    implementation(libs.dotenv.kotlin)

    implementation(libs.jda)
    implementation(libs.botcommands)
    implementation(libs.jda.ktx)

    implementation(libs.postgresql)
    implementation(libs.hikaricp)
    implementation(libs.bundles.flyway)

    implementation(libs.gson)
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.content.negotiation)

    implementation(libs.ktor.serialization.kotlinx.json)

    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.server.netty)

    implementation(libs.ktor.serialization.gson)

    implementation(libs.jsoup)

    implementation(libs.remark.java)

    implementation(libs.javaparser.symbol.solver.core)

    implementation(libs.palantir.java.format)

    testImplementation(libs.javassist)
    testImplementation(libs.junit.jupiter)
}
