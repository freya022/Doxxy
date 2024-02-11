package com.freya02.docs

import com.freya02.bot.utils.Utils.isBCGuild
import net.dv8tion.jda.api.entities.Guild

enum class DocSourceType(
    val id: Int,
    val cmdName: String,
    val sourceUrl: String,
    val sourceFolderName: String?,
    private val onlineURL: String?,
    vararg validPackagePatterns: String
) {
    JDA(
        1,
        "jda",
        "http://localhost:25566/JDA",
        "JDA",
        "https://docs.jda.wiki",
        "net\\.dv8tion\\.jda.*"
    ),
    BOT_COMMANDS(
        2,
        "botcommands",
        "http://localhost:25566/BotCommands",
        null,
        "https://freya022.github.io/BotCommands",
        "com\\.freya02\\.botcommands\\.api.*"
    ),
    JAVA(
        3,
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

    fun isValidPackage(packageName: String?): Boolean {
        if (packageName == null) return false

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

        fun typesForGuild(guild: Guild): List<DocSourceType> = when {
            guild.isBCGuild() -> entries
            else -> entries - BOT_COMMANDS
        }
    }
}