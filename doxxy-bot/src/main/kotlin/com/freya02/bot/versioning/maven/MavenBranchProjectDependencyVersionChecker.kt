package com.freya02.bot.versioning.maven

import com.freya02.bot.versioning.ArtifactInfo
import com.freya02.bot.versioning.LibraryType

class MavenBranchProjectDependencyVersionChecker(
    latest: ArtifactInfo,
    githubOwnerName: String,
    githubRepoName: String,
    targetArtifactId: String,
    private val targetBranchName: String,
) : MavenProjectDependencyVersionChecker(latest, githubOwnerName, githubRepoName, targetArtifactId) {
    constructor(
        latest: ArtifactInfo,
        targetLibrary: LibraryType,
        targetArtifactId: String,
        targetBranchName: String,
    ) : this(latest, targetLibrary.githubOwnerName, targetLibrary.githubRepoName, targetArtifactId, targetBranchName)

    override fun getTargetBranchName(): String {
        return targetBranchName
    }
}