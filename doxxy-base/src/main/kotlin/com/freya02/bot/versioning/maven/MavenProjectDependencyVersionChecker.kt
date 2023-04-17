package com.freya02.bot.versioning.maven

import com.freya02.bot.versioning.VersionChecker
import com.freya02.bot.versioning.github.GithubUtils
import java.io.IOException
import java.nio.file.Path

open class MavenProjectDependencyVersionChecker(
    lastSavedPath: Path,
    private val ownerName: String,
    private val artifactId: String,
    private val targetArtifactId: String
) : VersionChecker(
    lastSavedPath
) {
    @Throws(IOException::class)
    override fun checkVersion(): Boolean {
        val targetBranchName = targetBranchName
        val latestDependencyVersion =
            MavenUtils.retrieveDependencyVersion(ownerName, artifactId, targetBranchName, targetArtifactId)
        val changed = latestDependencyVersion != diskLatest

        latest = latestDependencyVersion

        return changed
    }

    @get:Throws(IOException::class)
    protected open val targetBranchName: String
        get() = GithubUtils.getLatestBranch(ownerName, artifactId).branchName
}