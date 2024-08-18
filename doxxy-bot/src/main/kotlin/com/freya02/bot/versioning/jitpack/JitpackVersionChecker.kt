package com.freya02.bot.versioning.jitpack

import com.freya02.bot.versioning.LibraryVersion
import com.freya02.bot.versioning.VersionChecker
import com.freya02.bot.versioning.github.GithubUtils

class JitpackVersionChecker(latest: LibraryVersion) : VersionChecker(latest) {
    override fun retrieveLatest(): LibraryVersion {
        val (_, groupId, artifactId) = latest
        val latestBranch = GithubUtils.getLatestBranch(groupId.substringAfter(".github."), artifactId)
        return latest.copy(version = latestBranch.latestCommitSha.asSha10)
    }
}