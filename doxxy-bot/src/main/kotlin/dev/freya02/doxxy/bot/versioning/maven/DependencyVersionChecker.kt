package dev.freya02.doxxy.bot.versioning.maven

import dev.freya02.doxxy.bot.utils.HttpUtils
import dev.freya02.doxxy.bot.versioning.LibraryVersion
import dev.freya02.doxxy.bot.versioning.VersionChecker

class DependencyVersionChecker(latest: LibraryVersion, private val targetArtifactId: String, private val pomUrlSupplier: () -> String) : VersionChecker(latest) {
    override suspend fun retrieveLatest(): LibraryVersion {
        val document = HttpUtils.getDocument(pomUrlSupplier())
        val artifactInfo = MavenUtils.parseDependencyVersion(document, targetArtifactId)
        return latest.copy(groupId = artifactInfo.groupId, artifactId = artifactInfo.artifactId, version = artifactInfo.version)
    }
}