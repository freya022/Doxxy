package com.freya02.bot.versioning.maven

import java.nio.file.Path

class MavenBranchProjectDependencyVersionChecker(
    lastSavedPath: Path,
    ownerName: String,
    artifactId: String,
    targetArtifactId: String,
    override val targetBranchName: String
) : MavenProjectDependencyVersionChecker(lastSavedPath, ownerName, artifactId, targetArtifactId)