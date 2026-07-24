package dev.freya02.doxxy.bot.versioning.maven

import dev.freya02.doxxy.bot.utils.HttpUtils
import dev.freya02.doxxy.bot.versioning.ArtifactInfo
import dev.freya02.doxxy.bot.versioning.LibraryVersion
import dev.freya02.doxxy.bot.versioning.VersionChecker
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.jsoup.nodes.Element
import tools.jackson.databind.JsonNode
import tools.jackson.databind.node.ObjectNode
import tools.jackson.databind.node.StringNode
import tools.jackson.dataformat.toml.TomlMapper

class DependencyVersionChecker(latest: LibraryVersion, private val targetArtifactId: String, private val pomUrlSupplier: () -> String) : VersionChecker(latest) {
    override suspend fun retrieveLatest(): LibraryVersion {
        val document = HttpUtils.getDocument(pomUrlSupplier())
        // For some reason, browsers and external HTTP clients do give the XML directly,
        // but the bot querying it gives us the file wrapped inside an HTML document...
        val root = document.selectFirst("html > body") ?: document

        val repoUrl = getRepoUrl(root)
        val gitTag = getGitTag(root)
        val versionCatalog = getVersionCatalog(repoUrl, gitTag)
        val targetDependency = getTargetDependency(versionCatalog)

        return latest.copy(groupId = targetDependency.groupId, artifactId = targetDependency.artifactId, version = targetDependency.version)
    }

    private fun getRepoUrl(root: Element): String {
        return root.selectFirst("project > scm > url")?.text()
            ?: throw IllegalArgumentException("Could not find repo URL")
    }

    private fun getGitTag(root: Element): String {
        return root.selectFirst("project > scm > tag")?.text()
            ?: throw IllegalArgumentException("Could not find Git tag")
    }

    private fun getVersionCatalog(repoUrl: String, gitTag: String): JsonNode {
        val versionCatalogUrl = HttpUrl.Builder()
            .scheme("https")
            .host("raw.githubusercontent.com")
            .addEncodedPathSegments(repoUrl.toHttpUrl().encodedPathSegments.joinToString("/"))
            .addPathSegments("refs/tags")
            .addPathSegment(gitTag)
            .addPathSegments("gradle/libs.versions.toml")
            .build()

        val versionCatalogBody = HttpUtils.downloadBody(versionCatalogUrl.toString())
        return TomlMapper.shared().readTree(versionCatalogBody)
    }

    private fun getTargetDependency(versionCatalog: JsonNode): ArtifactInfo {
        val libEntry = run {
            val libraries = versionCatalog["libraries"]?.asObject() ?: throw IllegalArgumentException("'libraries' object not found")
            libraries.firstOrNull { it["module"].asString().endsWith(targetArtifactId) }
                ?: throw IllegalArgumentException("'$targetArtifactId' library not found")
        }

        val module = libEntry["module"]!!.asString()
        val version = when (val version = libEntry["version"]) {
            is ObjectNode -> versionCatalog.getVersionByRef(version["ref"]!!.asString())
            is StringNode -> version.asString()
            else -> throw IllegalArgumentException("'version' property has an unexpected type: ${version.nodeType}")
        }

        return ArtifactInfo(
            groupId = module.substringBefore(':'),
            artifactId = module.substringAfter(':'),
            version = version,
        )
    }

    private fun JsonNode.getVersionByRef(ref: String): String {
        val versions = this["versions"] ?: throw IllegalArgumentException("'versions' object not found")
        return versions[ref]?.asString() ?: throw IllegalArgumentException("'$ref' version not found")
    }
}
