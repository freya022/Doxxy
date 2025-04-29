package dev.freya02.doxxy.bot.versioning.supplier

enum class BuildToolType(
    val humanName: String,
    val cmdName: String,
    val folderName: String,
    val blockLang: String,
    val fileExtension: String,
    val fileName: String
) {
    MAVEN("Maven", "maven", "maven", "xml", "xml", "pom"),
    GRADLE("Gradle", "gradle", "gradle", "gradle", "gradle", "build"),
    GRADLE_KTS("Kotlin Gradle", "kotlin_gradle", "gradle_kts", "gradle", "gradle.kts", "build");
}