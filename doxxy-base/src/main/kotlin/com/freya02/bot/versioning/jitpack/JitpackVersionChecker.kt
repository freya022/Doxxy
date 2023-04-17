package com.freya02.bot.versioning.jitpack

import com.freya02.bot.versioning.ArtifactInfo
import com.freya02.bot.versioning.VersionChecker
import com.freya02.bot.versioning.github.GithubUtils
import java.io.IOException
import java.nio.file.Path

class JitpackVersionChecker(
    lastSavedPath: Path,
    private val ownerName: String,
    private val groupId: String,
    private val artifactId: String
) : VersionChecker(
    lastSavedPath
) {
    @Throws(IOException::class)
    override fun checkVersion(): Boolean {
        val latestBranch = GithubUtils.getLatestBranch(ownerName, artifactId)
        val latestVersion = ArtifactInfo(
            groupId,
            artifactId,
            latestBranch.latestCommitSha.asSha10
        )

        val changed = latestVersion != diskLatest

        latest = latestVersion

        return changed
    }
}