package dev.freya02.doxxy.bot.versioning.maven

import dev.freya02.doxxy.bot.utils.HttpUtils
import dev.freya02.doxxy.bot.versioning.ArtifactInfo
import net.dv8tion.jda.api.exceptions.ParsingException
import org.jsoup.nodes.Document
import java.io.IOException

object MavenUtils {
    @Throws(IOException::class)
    fun getLatestStableMavenVersion(repoType: RepoType, groupId: String, artifactId: String): String {
        val mavenMetadata = getMavenMetadata(repoType.urlFormat, groupId, artifactId)
        val latest = mavenMetadata.selectFirst("metadata > versioning > latest")
        return latest?.text() ?: throw ParsingException("Unable to parse latest version")
    }

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

    @Throws(IOException::class)
    private fun getMavenMetadata(formatUrl: String, groupId: String, artifactId: String): Document {
        return HttpUtils.getDocument(formatUrl.format(groupId.replace('.', '/'), artifactId))
    }
}