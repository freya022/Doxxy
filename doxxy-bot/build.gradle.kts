plugins {
    id("doxxy-conventions")
    id("doxxy-app-conventions")
}

group = "io.github.freya022"

dependencies {
    api(projects.doxxy.doxxyCommons)

    api(libs.kotlinx.coroutines.core)
    api(libs.kotlinx.coroutines.debug)
    api(libs.stacktrace.decoroutinator)

    api(libs.dotenv.kotlin)

    api(libs.jda)
    api(libs.botcommands)
    api(libs.jda.ktx)

    api(libs.bundles.slf4j)
    api(libs.logback.classic)

    api(libs.postgresql)
    api(libs.hikaricp)
    api(libs.bundles.flyway)

    api(libs.gson)
    api(libs.kotlinx.serialization.json)

    api(libs.jsoup)

    api(libs.remark.java)

    api(libs.javaparser.symbol.solver.core)

    api(libs.palantir.java.format)

    api(libs.ktor.client.core)
    api(libs.ktor.client.okhttp)
    api(libs.ktor.client.content.negotiation)

    api(libs.ktor.serialization.kotlinx.json)

    api(libs.ktor.server.core)
    api(libs.ktor.server.content.negotiation)
    api(libs.ktor.server.netty)

    api(libs.ktor.serialization.gson)

    testImplementation(libs.javassist)
    testImplementation(libs.junit.jupiter)
}
