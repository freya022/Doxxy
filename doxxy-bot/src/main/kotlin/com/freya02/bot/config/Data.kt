package com.freya02.bot.config

import com.freya02.bot.utils.CryptoUtils
import com.freya02.bot.versioning.VersionType
import com.freya02.bot.versioning.github.GithubBranch
import com.freya02.docs.DocSourceType
import java.io.FileNotFoundException
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.createDirectories
import kotlin.io.path.notExists

object Data {
    /**
     * Where your bot can write data if needed
     */
    val folder: Path = Environment.folder.resolve(if (Environment.isDev) "dev-data" else "data")

    val javadocsPath: Path = folder.resolve("javadocs")
    val jdaForkPath: Path = folder.resolve("JDA-Fork")
    private val lastKnownVersionsFolderPath: Path = folder.resolve("last_versions")
    private val branchVersionsFolderPath: Path = lastKnownVersionsFolderPath.resolve("branch_versions")
    private val pageCacheFolderPath: Path = folder.resolve("page_cache")

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

    /**
     * Checks whether the path exists, throwing if not.
     */
    private fun Path.validatedPath(desc: String): Path = this.also {
        if (it.notExists())
            throw FileNotFoundException("$desc at ${it.absolutePathString()} does not exist.")
    }
}