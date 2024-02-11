package com.freya02.bot.versioning

enum class FileType(val fileSuffix: String) {
    CLASSES(""),
    JAVADOC("-javadoc"),
    SOURCES("-sources")
}