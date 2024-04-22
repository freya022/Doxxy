package com.freya02.bot.versioning.maven

import com.freya02.bot.utils.HttpUtils
import com.freya02.bot.versioning.ArtifactInfo
import com.freya02.bot.versioning.LibraryVersion
import com.freya02.bot.versioning.VersionChecker

class DependencyVersionChecker(latest: LibraryVersion, private val targetArtifactId: String, private val pomUrlSupplier: () -> String) : VersionChecker(latest) {
    override fun retrieveLatest(): ArtifactInfo {
        val document = HttpUtils.getDocument(pomUrlSupplier())
        val dependencyVersion = MavenUtils.parseDependencyVersion(document, targetArtifactId)
        return latest.copy(version = dependencyVersion)
    }
}