package dev.freya02.doxxy.docs

import dev.freya02.doxxy.common.Directories
import dev.freya02.doxxy.common.DocumentedExampleLibrary
import java.nio.file.Path

//TODO this shouldn't be in the docs module
// move this to the bot, then pass sourceUrl, onlineURL and packagePatterns when creating docs.
// Depends on ClassDocs being referenced in all BaseDoc
enum class DocSourceType(
    val id: Int,
    val cmdName: String,
    val textArg: String,
    val sourceUrl: String,
    val sourceFolderName: String?,
    private val onlineURL: String?,
    vararg validPackagePatterns: String
) {
    JDA(
        1,
        "jda",
        "jda",
        "http://localhost:25566/JDA",
        "JDA",
        "https://docs.jda.wiki",
        "net\\.dv8tion\\.jda.*"
    ),
    JAVA(
        3,
        "java",
        "java",
        "https://docs.oracle.com/en/java/javase/17/docs/api",
        null,
        "https://docs.oracle.com/en/java/javase/17/docs/api",
        "java\\.io.*",
        "java\\.lang",
        "java\\.lang\\.annotation.*",
        "java\\.lang\\.invoke.*",
        "java\\.lang\\.reflect.*",
        "java\\.math.*",
        "java\\.nio",
        "java\\.nio\\.channels",
        "java\\.nio\\.file",
        "java\\.sql.*",
        "java\\.time.*",
        "java\\.text.*",
        "java\\.security.*",
        "java\\.util",
        "java\\.util\\.concurrent.*",
        "java\\.util\\.function",
        "java\\.util\\.random",
        "java\\.util\\.regex",
        "java\\.util\\.stream"
    );

    private val validPackagePatterns: List<Regex> = validPackagePatterns.map { it.toRegex() }

    val allClassesIndexURL: String = "$sourceUrl/allclasses-index.html"
    val constantValuesURL: String = "$sourceUrl/constant-values.html"

    fun toEffectiveURL(url: String): String {
        if (onlineURL == null) return url

        return when {
            url.startsWith(sourceUrl) -> onlineURL + url.substring(sourceUrl.length)
            else -> url
        }
    }

    fun toOnlineURL(url: String): String? {
        if (onlineURL == null) return null

        return when {
            url.startsWith(sourceUrl) -> onlineURL + url.substring(sourceUrl.length)
            else -> url
        }
    }

    fun isValidPackage(packageName: String): Boolean {
        return validPackagePatterns.any { packageName.matches(it) }
    }

    companion object {
        fun fromId(id: Int): DocSourceType {
            return entries.find { it.id == id } ?: throw IllegalArgumentException("Unknown source ID $id")
        }

        fun fromIdOrNull(id: Int): DocSourceType? {
            return entries.find { it.id == id }
        }

        fun fromUrl(url: String): DocSourceType? {
            return entries.find { source -> url.startsWith(source.sourceUrl) || source.onlineURL != null && url.startsWith(source.onlineURL) }
        }

        fun DocumentedExampleLibrary.toDocSourceType(): DocSourceType = when (this) {
            DocumentedExampleLibrary.JDA -> JDA
            DocumentedExampleLibrary.JDK -> JAVA
        }
    }
}

val DocSourceType.cacheDirectory: Path
    get() = Directories.pageCache.resolve(name)

val DocSourceType.sourceDirectory: Path?
    get() = sourceFolderName?.let { Directories.javadocs.resolve(it) }

val DocSourceType.javadocDirectory: Path
    get() = Directories.javadocs.resolve(name)