package dev.freya02.doxxy.bot.versioning.maven

import dev.freya02.doxxy.bot.versioning.LibraryVersion
import dev.freya02.doxxy.bot.versioning.VersionChecker

class MavenVersionChecker(
    latest: LibraryVersion,
    private val repoType: RepoType,
) : VersionChecker(latest) {
    override fun retrieveLatest(): LibraryVersion {
        return latest.copy(version = MavenUtils.getLatestStableMavenVersion(repoType, latest.groupId, latest.artifactId))
    }
}