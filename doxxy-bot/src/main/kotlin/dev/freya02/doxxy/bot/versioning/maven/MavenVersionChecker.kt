package dev.freya02.doxxy.bot.versioning.maven

import dev.freya02.doxxy.bot.versioning.LibraryVersion
import dev.freya02.doxxy.bot.versioning.VersionChecker

class MavenVersionChecker(
    private val repositoryClient: MavenRepositoryClient,
    latest: LibraryVersion,
) : VersionChecker(latest) {
    override suspend fun retrieveLatest(): LibraryVersion {
        return latest.copy(version = repositoryClient.getLatestStableMavenVersion(latest.groupId, latest.artifactId))
    }
}