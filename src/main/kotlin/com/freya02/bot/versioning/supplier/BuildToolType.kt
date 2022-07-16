package com.freya02.bot.versioning.supplier

enum class BuildToolType(val humanName: String, val folderName: String) {
    MAVEN("Maven", "maven"),
    GRADLE("Gradle", "gradle"),
    GRADLE_KTS("Kotlin Gradle", "gradle_kts");
}