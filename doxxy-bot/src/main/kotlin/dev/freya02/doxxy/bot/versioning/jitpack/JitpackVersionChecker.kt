package dev.freya02.doxxy.bot.versioning.jitpack

import dev.freya02.doxxy.bot.versioning.LibraryVersion
import dev.freya02.doxxy.bot.versioning.VersionChecker
import dev.freya02.doxxy.bot.versioning.github.GithubUtils

class JitpackVersionChecker(latest: LibraryVersion) : VersionChecker(latest) {
    override fun retrieveLatest(): LibraryVersion {
        val (_, groupId, artifactId) = latest
        val latestBranch = GithubUtils.getLatestBranch(groupId.substringAfter(".github."), artifactId)
        return latest.copy(version = latestBranch.latestCommitSha.asSha10)
    }
}