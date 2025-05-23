plugins {
    id("doxxy-conventions")
    id("doxxy-app-conventions")

    alias(libs.plugins.kotlinx.serialization)
}

dependencies {
    implementation(projects.doxxy.doxxyCommons)
    implementation(projects.doxxy.doxxyDocs)
    implementation(projects.doxxy.doxxyGithubClient)

    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.debug)
    implementation(libs.stacktrace.decoroutinator)

    implementation(libs.bundles.slf4j)
    implementation(libs.logback.classic)

    implementation(libs.jda)
    implementation(libs.botcommands)
    implementation(libs.jda.ktx)

    implementation(libs.postgresql)
    implementation(libs.hikaricp)
    implementation(libs.bundles.flyway)

    implementation(libs.kotlinx.serialization.json)

    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.content.negotiation)

    implementation(libs.ktor.serialization.kotlinx.json)

    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.server.netty)

    implementation(libs.jsoup)

    implementation(libs.remark.java)

    implementation(libs.javaparser.symbol.solver.core)

    implementation(libs.palantir.java.format)

    testImplementation(libs.javassist)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.mockk)
    testImplementation(libs.bytebuddy)
}

jib {
    from {
        image = "docker://doxxy-bot-eclipse-temurin:24-jdk"
    }

    to {
        image = "ghcr.io/freya022/doxxy-bot"
    }

    container {
        mainClass = "dev.freya02.doxxy.bot.Main"
        jvmFlags = listOf(
            "--add-exports", "jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED",
            "--add-exports", "jdk.compiler/com.sun.tools.javac.file=ALL-UNNAMED",
            "--add-exports", "jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED",
            "--add-exports", "jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED",
            "--add-exports", "jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED",
        )
    }
}
