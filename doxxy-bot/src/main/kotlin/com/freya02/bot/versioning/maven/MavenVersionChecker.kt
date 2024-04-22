package com.freya02.bot.versioning.maven

import com.freya02.bot.versioning.ArtifactInfo
import com.freya02.bot.versioning.LibraryVersion
import com.freya02.bot.versioning.VersionChecker

class MavenVersionChecker(
    latest: LibraryVersion,
    private val repoType: RepoType,
) : VersionChecker(latest) {
    override fun retrieveLatest(): ArtifactInfo {
        return latest.copy(version = MavenUtils.getLatestStableMavenVersion(repoType, latest.groupId, latest.artifactId))
    }
}