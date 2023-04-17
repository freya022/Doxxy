package com.freya02.bot.versioning

import java.io.IOException
import java.nio.file.Path
import kotlin.io.path.notExists
import kotlin.io.path.readLines

//see https://central.sonatype.org/search/rest-api-guide/
private const val MAVEN_URL_FORMAT = "https://search.maven.org/remotecontent?filepath=%s/%s/%s/%s-%s%s.jar"
private const val JITPACK_URL_FORMAT = "https://jitpack.io/%s/%s/%s/%s-%s%s.jar"

data class ArtifactInfo(val groupId: String, val artifactId: String, val version: String) {
    fun toFileString(): String {
        return arrayOf(groupId, artifactId, version).joinToString("\n")
    }

    fun toMavenUrl(fileType: FileType): String {
        return MAVEN_URL_FORMAT.format(
            groupId.replace('.', '/'),
            artifactId,
            version,
            artifactId,
            version,
            fileType.fileSuffix
        )
    }

    fun toJitpackUrl(fileType: FileType): String {
        return JITPACK_URL_FORMAT.format(
            groupId.replace('.', '/'),
            artifactId,
            version,
            artifactId,
            version,
            fileType.fileSuffix
        )
    }

    override fun toString(): String {
        return "ArtifactInfo[" +
                "groupId=" + groupId + ", " +
                "artifactId=" + artifactId + ", " +
                "version=" + version + ']'
    }

    companion object {
        @Throws(IOException::class)
        fun fromFileString(path: Path): ArtifactInfo {
            if (path.notExists()) return ArtifactInfo("invalid", "invalid", "invalid")

            val lines = path.readLines()
            return when {
                lines.size != 3 -> ArtifactInfo("invalid", "invalid", "invalid")
                else -> ArtifactInfo(lines[0], lines[1], lines[2])
            }
        }
    }
}