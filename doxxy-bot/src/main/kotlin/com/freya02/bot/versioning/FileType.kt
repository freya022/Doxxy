package com.freya02.bot.versioning

enum class FileType(val fileSuffix: String) {
    POM(".pom"),
    CLASSES(".jar"),
    JAVADOC("-javadoc.jar"),
    SOURCES("-sources.jar"),
}