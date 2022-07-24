package com.freya02.bot.versioning.maven

import com.freya02.bot.utils.HttpUtils
import com.freya02.bot.versioning.ArtifactInfo
import net.dv8tion.jda.api.exceptions.ParsingException
import org.jsoup.nodes.Document
import java.io.IOException

object MavenUtils {
    @Throws(IOException::class)
    fun getLatestMavenVersion(formatUrl: String, groupId: String, artifactId: String?): String {
        return getMavenMetadata(formatUrl, groupId, artifactId)
            .selectFirst("metadata > versioning > latest")
            ?.text() ?: throw ParsingException("Unable to parse latest version")
    }

    @Throws(IOException::class)
    fun retrieveDependencyVersion(
        ownerName: String,
        artifactId: String,
        branchName: String,
        targetArtifactId: String
    ): ArtifactInfo {
        val document = HttpUtils.getDocument(
            "https://raw.githubusercontent.com/%s/%s/%s/pom.xml".format(
                ownerName,
                artifactId,
                branchName
            )
        )

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

                return ArtifactInfo(
                    groupIdElement.text(),
                    artifactIdElement.text(),
                    versionElement.text()
                )
            }

        throw IOException("Unable to get dependency version from " + document.baseUri())
    }

    @Throws(IOException::class)
    private fun getMavenMetadata(formatUrl: String, groupId: String, artifactId: String?): Document {
        return HttpUtils.getDocument(formatUrl.format(groupId.replace('.', '/'), artifactId))
    }
}