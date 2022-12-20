package com.freya02.bot

import java.io.FileNotFoundException
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.createDirectories
import kotlin.io.path.notExists

// If test configs exists then they should be loaded
// If not then fallback to the config path, which must be validated
// Other configs such as logback are set in the bot folder
object Data {
    val botFolder: Path = Path(System.getProperty("user.home"), "Bots", "Doxxy")
    private val configFolder: Path = botFolder.resolve("config")

    //Fine if not used, might just be using the test config
    val configPath: Path by lazy { validatedPath("Bot config", configFolder.resolve("Config.json")) }
    val testConfigPath: Path = Path("Test_Config.json")

    val logbackConfigPath: Path = validatedPath("Logback config", configFolder.resolve("logback.xml"))

    val javadocsPath: Path = botFolder.resolve("javadocs")
    val lastKnownVersionsFolderPath: Path = botFolder.resolve("last_versions")
    val branchVersionsFolderPath: Path = lastKnownVersionsFolderPath.resolve("branch_versions")
    val pageCacheFolderPath: Path = botFolder.resolve("page_cache")

    fun init() {
        lastKnownVersionsFolderPath.createDirectories()
        branchVersionsFolderPath.createDirectories()
        pageCacheFolderPath.createDirectories()
    }

    private fun validatedPath(desc: String, p: Path): Path {
        if (p.notExists())
            throw FileNotFoundException("$desc at ${p.absolutePathString()} does not exist.")
        return p
    }
}