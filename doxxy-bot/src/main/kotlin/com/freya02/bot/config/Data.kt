package com.freya02.bot.config

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
    private val pageCacheFolderPath: Path = folder.resolve("page_cache")

    fun init() {
        pageCacheFolderPath.createDirectories()
    }

    val jdaDocsFolder: Path = javadocsPath.resolve("JDA")

    fun getCacheFolder(docSourceType: DocSourceType): Path = pageCacheFolderPath.resolve(docSourceType.name)

    /**
     * Checks whether the path exists, throwing if not.
     */
    private fun Path.validatedPath(desc: String): Path = this.also {
        if (it.notExists())
            throw FileNotFoundException("$desc at ${it.absolutePathString()} does not exist.")
    }
}