package dev.freya02.doxxy.bot.versioning

import dev.freya02.doxxy.bot.utils.HttpUtils
import java.io.IOException
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.createTempFile
import kotlin.io.path.deleteRecursively
import kotlin.io.path.extension
import kotlin.streams.asSequence

object VersionsUtils {
    @Throws(IOException::class)
    @OptIn(ExperimentalPathApi::class)
    fun replaceWithZipContent(tempZip: Path, targetDocsFolder: Path, extension: String) {
        if (Files.exists(targetDocsFolder)) {
            targetDocsFolder.deleteRecursively()
        }

        extractZip(tempZip, targetDocsFolder, extension)
    }

    fun extractZip(tempZip: Path, targetDocsFolder: Path, extension: String) {
        FileSystems.newFileSystem(tempZip).use { zfs ->
            val zfsRoot = zfs.getPath("/")

            Files.walk(zfsRoot)
                .asSequence()
                .filter { path: Path -> Files.isRegularFile(path) }
                .filter { p: Path -> p.fileName.extension == extension }
                .forEach { sourcePath ->
                    val targetPath = targetDocsFolder.resolve(zfsRoot.relativize(sourcePath).toString())
                    Files.createDirectories(targetPath.parent)
                    Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING)
                }
        }
    }

    fun ArtifactInfo.downloadMavenJavadoc(): Path {
        return downloadJavadoc(this.toMavenUrl(FileType.JAVADOC))
    }

    fun ArtifactInfo.downloadJitpackJavadoc(): Path {
        return downloadJavadoc(this.toJitpackUrl(FileType.JAVADOC))
    }

    fun ArtifactInfo.downloadMavenSources(): Path {
        return downloadSources(this.toMavenUrl(FileType.SOURCES))
    }

    fun ArtifactInfo.downloadJitpackSources(): Path {
        return downloadSources(this.toJitpackUrl(FileType.SOURCES))
    }

    private fun ArtifactInfo.downloadJavadoc(url: String): Path {
        return HttpUtils.downloadAt(url, createTempFile("${this.artifactId}-javadoc", ".zip"))
    }

    private fun ArtifactInfo.downloadSources(url: String): Path {
        return HttpUtils.downloadAt(url, createTempFile("${this.artifactId}-sources", ".zip"))
    }
}