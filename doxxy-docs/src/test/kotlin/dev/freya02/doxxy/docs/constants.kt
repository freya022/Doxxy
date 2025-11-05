package dev.freya02.doxxy.docs

import dev.freya02.doxxy.docs.JavadocSource.PackageMatcher.Companion.recursive
import dev.freya02.doxxy.docs.JavadocSource.PackageMatcher.Companion.single

const val JDA_HOST = "localhost"
const val JDA_PORT = 25566
const val JDA_PATH = "/JDA"

val JDA_SOURCE = JavadocSource(
    name = "JDA",
    sourceUrl = "http://$JDA_HOST:$JDA_PORT$JDA_PATH",
    onlineURL = "https://docs.jda.wiki",
    packageMatchers = listOf(
        recursive("net.dv8tion.jda")
    )
)

val JDK_SOURCE = JavadocSource(
    name = "JDK",
    sourceUrl = "https://docs.oracle.com/en/java/javase/25/docs/api",
    onlineURL = "https://docs.oracle.com/en/java/javase/25/docs/api",
    packageMatchers = listOf(
        recursive("java.io"),
        single("java.lang"),
        recursive("java.lang.annotation"),
        recursive("java.lang.invoke"),
        recursive("java.lang.reflect"),
        recursive("java.math"),
        single("java.nio"),
        single("java.nio.channels"),
        single("java.nio.file"),
        recursive("java.sql"),
        recursive("java.time"),
        recursive("java.text"),
        recursive("java.security"),
        single("java.util"),
        recursive("java.util.concurrent"),
        single("java.util.function"),
        single("java.util.random"),
        single("java.util.regex"),
        single("java.util.stream")
    )
)
