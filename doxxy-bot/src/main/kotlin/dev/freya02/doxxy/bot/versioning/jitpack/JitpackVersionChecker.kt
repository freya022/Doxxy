package dev.freya02.doxxy.bot.versioning.jitpack

import dev.freya02.doxxy.bot.versioning.LibraryVersion
import dev.freya02.doxxy.bot.versioning.VersionChecker
import dev.freya02.doxxy.github.client.GithubClient
import kotlinx.coroutines.flow.toList

class JitpackVersionChecker(private val githubClient: GithubClient, latest: LibraryVersion) : VersionChecker(latest) {
    override suspend fun retrieveLatest(): LibraryVersion {
        val (_, groupId, artifactId) = latest
        // TODO make retrieveLatest suspend and replace with `run`
        val latestBranch = run {
            val branches = githubClient.getBranches(groupId.substringAfter(".github."), artifactId, perPage = 100).toList()
            val mostRecentVersionedBranch = branches
                .filter { it.name.matches("\\d\\.\\d\\.\\d".toRegex()) }
                .maxByOrNull { it.name }
            mostRecentVersionedBranch ?: branches[0]
        }
        return latest.copy(version = latestBranch.commit.sha.asSha10)
    }
}