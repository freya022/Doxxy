package com.freya02.bot.versioning

import com.freya02.bot.utils.HttpUtils
import com.freya02.bot.utils.Utils.deleteRecursively
import okhttp3.Request
import java.io.IOException
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlin.io.path.createTempFile
import kotlin.io.path.extension
import kotlin.io.path.writeBytes
import kotlin.streams.asSequence

object VersionsUtils {
    @Throws(IOException::class)
    fun replaceWithZipContent(tempZip: Path, targetDocsFolder: Path) {
        if (Files.exists(targetDocsFolder)) {
            targetDocsFolder.deleteRecursively()
        }

        FileSystems.newFileSystem(tempZip).use { zfs ->
            val zfsRoot = zfs.getPath("/")

            Files.walk(zfsRoot)
                .asSequence()
                .filter { path: Path -> Files.isRegularFile(path) }
                .filter { p: Path -> p.fileName.extension == "html" }
                .forEach { sourcePath ->
                    val targetPath = targetDocsFolder.resolve(zfsRoot.relativize(sourcePath).toString())
                    Files.createDirectories(targetPath.parent)
                    Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING)
                }
        }
    }

    fun ArtifactInfo.downloadJitpackJavadoc(): Path {
        return createTempFile("${this.artifactId}-javadoc", ".zip")
            .also { path ->
                HttpUtils.CLIENT
                    .newCall(Request.Builder().url(this.toJitpackUrl()).build())
                    .execute()
                    .use {
                        path.writeBytes(it.body!!.bytes())
                    }
            }
    }
}