package com.freya02.bot.versioning.maven

import com.freya02.bot.versioning.ArtifactInfo
import com.freya02.bot.versioning.VersionChecker
import com.freya02.bot.versioning.github.GithubUtils

open class MavenProjectDependencyVersionChecker(
    latest: ArtifactInfo,
    private val repoOwnerName: String,
    private val repoName: String,
    private val targetArtifactId: String
) : VersionChecker(latest) {
    override fun retrieveLatest(): ArtifactInfo {
        return latest.copy(version = MavenUtils.retrieveGithubDependencyVersion(repoOwnerName, repoName, getTargetBranchName(), targetArtifactId))
    }

    protected open fun getTargetBranchName(): String = GithubUtils.getLatestBranch(repoOwnerName, repoName).branchName
}