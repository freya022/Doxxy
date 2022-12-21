package com.freya02.bot.versioning.supplier

enum class BuildToolType(val humanName: String, val cmdName: String, val folderName: String, val blockLang: String, val fileExtension: String) {
    MAVEN("Maven",  "maven", "maven", "xml", "xml"),
    GRADLE("Gradle", "gradle", "gradle","gradle", "gradle"),
    GRADLE_KTS("Kotlin Gradle", "kotlin_gradle", "gradle_kts", "gradle", "gradle.kts");
}