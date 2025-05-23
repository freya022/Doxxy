package dev.freya02.doxxy.bot.versioning

enum class FileType(val fileSuffix: String) {
    POM(".pom"),
    CLASSES(".jar"),
    JAVADOC("-javadoc.jar"),
    SOURCES("-sources.jar"),
}