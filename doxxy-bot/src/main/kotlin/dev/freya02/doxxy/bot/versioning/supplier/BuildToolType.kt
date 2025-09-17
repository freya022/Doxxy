package dev.freya02.doxxy.bot.versioning.supplier

enum class BuildToolType(
    val humanName: String,
    val folderName: String,
    val blockLang: String,
    val fileExtension: String,
    val fileName: String
) {
    MAVEN("Maven", "maven", "xml", "xml", "pom"),
    GRADLE("Gradle", "gradle", "gradle", "gradle", "build"),
    GRADLE_KTS("Kotlin Gradle", "gradle_kts", "gradle", "gradle.kts", "build");
}
