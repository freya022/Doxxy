package com.freya02.bot.versioning.maven

import com.freya02.bot.versioning.LibraryType
import java.nio.file.Path

class MavenBranchProjectDependencyVersionChecker(
    lastSavedPath: Path,
    githubOwnerName: String,
    githubRepoName: String,
    targetArtifactId: String,
    override val targetBranchName: String
) : MavenProjectDependencyVersionChecker(lastSavedPath, githubOwnerName, githubRepoName, targetArtifactId) {
    constructor(
        lastSavedPath: Path,
        project: LibraryType,
        targetArtifactId: String,
        targetBranchName: String
    ) : this(lastSavedPath, project.githubOwnerName, project.githubRepoName, targetArtifactId, targetBranchName)
}