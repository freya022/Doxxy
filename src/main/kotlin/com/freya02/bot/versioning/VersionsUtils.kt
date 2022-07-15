package com.freya02.bot.versioning

import com.freya02.bot.utils.HttpUtils
import com.freya02.bot.utils.Utils.deleteRecursively
import okhttp3.Request
import okhttp3.ResponseBody
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

    fun ArtifactInfo.downloadMavenJavadoc(): Path {
        return downloadJavadoc(this.toMavenJavadocUrl())
    }

    fun ArtifactInfo.downloadJitpackJavadoc(): Path {
        return downloadJavadoc(this.toJitpackJavadocUrl())
    }

    private fun ArtifactInfo.downloadJavadoc(url: String): Path {
        return createTempFile("${this.artifactId}-javadoc", ".zip")
            .also { path ->
                HttpUtils.CLIENT
                    .newCall(Request.Builder().url(url).build())
                    .execute()
                    .use { response ->
                        val body: ResponseBody = response.body
                            ?: throw IOException("Got no ResponseBody for ${response.request.url}")

                        if (!response.isSuccessful) throw IOException("Got an unsuccessful response from ${response.request.url}, code: ${response.code}")

                        path.writeBytes(body.bytes())
                    }
            }
    }
}