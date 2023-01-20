package com.freya02.bot

import com.freya02.bot.utils.CryptoUtils
import com.freya02.bot.versioning.VersionType
import com.freya02.bot.versioning.github.GithubBranch
import com.freya02.docs.DocSourceType
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
    private val botFolder: Path = validatedPath("Bot folder", Path(System.getProperty("user.home"), "Bots", "Doxxy"))
    private val configFolder: Path = botFolder.resolve("config")

    //Fine if not used, might just be using the test config
    val configPath: Path by lazy { validatedPath("Bot config", configFolder.resolve("Config.json")) }
    val testConfigPath: Path = Path("Test_Config.json")

    val logbackConfigPath: Path = validatedPath("Logback config", configFolder.resolve("logback.xml"))

    val javadocsPath: Path = botFolder.resolve("javadocs")
    private val lastKnownVersionsFolderPath: Path = botFolder.resolve("last_versions")
    private val branchVersionsFolderPath: Path = lastKnownVersionsFolderPath.resolve("branch_versions")
    private val pageCacheFolderPath: Path = botFolder.resolve("page_cache")

    fun init() {
        lastKnownVersionsFolderPath.createDirectories()
        branchVersionsFolderPath.createDirectories()
        pageCacheFolderPath.createDirectories()
    }

    val jdaDocsFolder: Path = javadocsPath.resolve("JDA")
    val bcDocsFolder: Path = javadocsPath.resolve("BotCommands")

    fun getCacheFolder(docSourceType: DocSourceType): Path = pageCacheFolderPath.resolve(docSourceType.name)

    fun getVersionPath(versionType: VersionType): Path = lastKnownVersionsFolderPath.resolve(versionType.fileName)

    fun getBranchFileName(branch: GithubBranch): Path = branchVersionsFolderPath.resolve(
        "%s-%s-%s.txt".format(
            branch.ownerName,
            branch.repoName,
            CryptoUtils.hash(branch.branchName)
        )
    )

    private fun validatedPath(desc: String, p: Path): Path {
        if (p.notExists())
            throw FileNotFoundException("$desc at ${p.absolutePathString()} does not exist.")
        return p
    }
}