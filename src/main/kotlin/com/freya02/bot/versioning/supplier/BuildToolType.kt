package com.freya02.bot.versioning.supplier

enum class BuildToolType(val humanName: String, val cmdName: String, val folderName: String, val blockLang: String) {
    MAVEN("Maven",  "maven", "maven", "xml"),
    GRADLE("Gradle", "gradle", "gradle","gradle"),
    GRADLE_KTS("Kotlin Gradle", "kotlin_gradle", "gradle_kts", "gradle");
}