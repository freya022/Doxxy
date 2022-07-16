package com.freya02.docs

enum class DocSourceType(
    val id: Int,
    val sourceUrl: String,
    private val onlineURL: String?,
    val githubSourceURL: String?,
    vararg validPackagePatterns: String
) {
    JDA(
        1,
        "http://localhost:25566/JDA",
        "https://ci.dv8tion.net/job/JDA5/javadoc",
        "https://github.com/DV8FromTheWorld/JDA/blob/master/src/main/java/",
        "net\\.dv8tion\\.jda.*"
    ),
    BOT_COMMANDS(
        2,
        "http://localhost:25566/BotCommands",
        null,
        null,
        "com\\.freya02\\.botcommands\\.api.*"
    ),
    JAVA(
        3,
        "https://docs.oracle.com/en/java/javase/17/docs/api",
        "https://docs.oracle.com/en/java/javase/17/docs/api",
        null,
        "java\\.io.*",
        "java\\.lang",
        "java\\.lang\\.annotation.*",
        "java\\.lang\\.invoke.*",
        "java\\.lang\\.reflect.*",
        "java\\.lang\\.reflect.*",
        "java\\.math.*",
        "java\\.nio",
        "java\\.nio\\.channels",
        "java\\.nio\\.file",
        "java\\.sql.*",
        "java\\.time.*",
        "java\\.util",
        "java\\.util\\.concurrent.*",
        "java\\.util\\.function",
        "java\\.util\\.random",
        "java\\.util\\.regex",
        "java\\.util\\.stream"
    );

    private val validPackagePatterns: List<Regex> = validPackagePatterns.map { it.toRegex() }

    val allClassesIndexURL: String = "$sourceUrl/allclasses-index.html"

    fun toOnlineURL(url: String): String {
        if (onlineURL == null) return url

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
        fun fromUrl(url: String): DocSourceType? {
            return values().find { source -> url.startsWith(source.sourceUrl) || source.onlineURL != null && url.startsWith(source.onlineURL) }
        }
    }
}