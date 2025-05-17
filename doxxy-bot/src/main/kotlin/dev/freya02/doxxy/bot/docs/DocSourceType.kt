package dev.freya02.doxxy.bot.docs

import dev.freya02.doxxy.common.Directories
import dev.freya02.doxxy.common.DocumentedExampleLibrary
import dev.freya02.doxxy.docs.JavadocSource
import dev.freya02.doxxy.docs.JavadocSource.PackageMatcher.Companion.recursive
import dev.freya02.doxxy.docs.JavadocSource.PackageMatcher.Companion.single
import java.nio.file.Path

enum class DocSourceType(
    val id: Int,
    val cmdName: String,
    val textArg: String,
    val sourceUrl: String,
    val sourceFolderName: String?,
    val onlineURL: String?,
    vararg val packageMatchers: JavadocSource.PackageMatcher
) {
    JDA(
        1,
        "jda",
        "jda",
        "http://localhost:25566/JDA",
        "JDA",
        "https://docs.jda.wiki",
        recursive("net.dv8tion.jda")
    ),
    JAVA(
        3,
        "java",
        "java",
        "https://docs.oracle.com/en/java/javase/17/docs/api",
        null,
        "https://docs.oracle.com/en/java/javase/17/docs/api",
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
        single("java.util.stream"),
    );

    fun toEffectiveURL(url: String): String {
        if (onlineURL == null) return url

        return when {
            url.startsWith(sourceUrl) -> onlineURL + url.substring(sourceUrl.length)
            else -> url
        }
    }

    companion object {
        fun fromId(id: Int): DocSourceType {
            return entries.find { it.id == id } ?: throw IllegalArgumentException("Unknown source ID $id")
        }

        fun fromIdOrNull(id: Int): DocSourceType? {
            return entries.find { it.id == id }
        }

        fun DocumentedExampleLibrary.toDocSourceType(): DocSourceType = when (this) {
            DocumentedExampleLibrary.JDA -> JDA
            DocumentedExampleLibrary.JDK -> JAVA
        }
    }
}

val DocSourceType.sourceDirectoryPath: Path?
    get() = sourceFolderName?.let { Directories.sources.resolve(it) }

val DocSourceType.javadocArchivePath: Path
    get() = Directories.javadocs.resolve("$name.zip")