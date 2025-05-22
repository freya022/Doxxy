package dev.freya02.doxxy.bot.versioning.maven

import dev.freya02.doxxy.bot.versioning.ArtifactInfo
import org.jsoup.nodes.Document
import java.io.IOException

object MavenUtils {

    fun parseDependencyVersion(
        document: Document,
        targetArtifactId: String,
    ): ArtifactInfo {
        document.select("project > dependencies > dependency")
            .forEachIndexed { i, element ->
                val groupIdElement = element.selectFirst("groupId")
                val artifactIdElement = element.selectFirst("artifactId")
                val versionElement = element.selectFirst("version")

                if (groupIdElement == null || artifactIdElement == null || versionElement == null) {
                    throw IllegalArgumentException("Could not parse dependency #$i at ${document.baseUri()}")
                }

                if (artifactIdElement.text() != targetArtifactId) {
                    return@forEachIndexed
                }

                return ArtifactInfo(groupIdElement.text(), artifactIdElement.text(), versionElement.text())
            }

        throw IOException("Unable to get dependency version from " + document.baseUri())
    }
}