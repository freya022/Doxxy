package com.freya02.bot.versioning

import java.io.IOException
import java.nio.file.Path
import kotlin.io.path.notExists
import kotlin.io.path.readLines

private const val MAVEN_JAVADOC_FORMAT = "https://repo1.maven.org/maven2/%s/%s/%s/%s-%s-javadoc.jar"
private const val JITPACK_JAVADOC_FORMAT = "https://jitpack.io/%s/%s/%s/%s-%s-javadoc.jar"

data class ArtifactInfo(val groupId: String, val artifactId: String, val version: String) {
    fun toFileString(): String {
        return arrayOf(groupId, artifactId, version).joinToString("\n")
    }

    fun toMavenJavadocUrl(): String {
        return MAVEN_JAVADOC_FORMAT.format(
            groupId.replace('.', '/'),
            artifactId,
            version,
            artifactId,
            version
        )
    }

    fun toJitpackJavadocUrl(): String {
        return JITPACK_JAVADOC_FORMAT.format(
            groupId.replace('.', '/'),
            artifactId,
            version,
            artifactId,
            version
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