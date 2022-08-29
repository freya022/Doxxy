package com.freya02.bot.versioning.supplier

enum class BuildToolType(val humanName: String, val cmdName: String, val folderName: String) {
    MAVEN("Maven",  "maven", "maven"),
    GRADLE("Gradle", "gradle", "gradle"),
    GRADLE_KTS("Kotlin Gradle", "kotlin_gradle", "gradle_kts");
}