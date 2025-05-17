package dev.freya02.doxxy.bot

import dev.freya02.doxxy.bot.docs.DocSourceType
import dev.freya02.doxxy.bot.docs.sourceDirectoryPath
import dev.freya02.doxxy.bot.versioning.VersionsUtils
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.exists
import kotlin.io.path.toPath

fun getResourcePath(resource: String): Path {
    return TestUtils::class.java.getResource(resource)!!.toURI().toPath()
}

fun extractJDASources(): Path {
    val tmpSourcesPath = Path(System.getProperty("java.io.tmpdir"), "JDA_sources")
    if (tmpSourcesPath.exists())
        return tmpSourcesPath

    VersionsUtils.extractZip(getResourcePath("/JDA-sources.jar"), DocSourceType.JDA.sourceDirectoryPath!!, "java")
    return tmpSourcesPath
}