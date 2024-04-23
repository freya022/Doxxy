package com.freya02.bot.versioning.maven

import com.freya02.bot.utils.HttpUtils
import com.freya02.bot.versioning.LibraryVersion
import com.freya02.bot.versioning.VersionChecker

class DependencyVersionChecker(latest: LibraryVersion, private val targetArtifactId: String, private val pomUrlSupplier: () -> String) : VersionChecker(latest) {
    override fun retrieveLatest(): String {
        val document = HttpUtils.getDocument(pomUrlSupplier())
        return MavenUtils.parseDependencyVersion(document, targetArtifactId)
    }
}