package com.freya02.bot.versioning.maven;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Path;

public class MavenBranchProjectDependencyVersionChecker extends MavenProjectDependencyVersionChecker {
	private final String targetBranch;

	public MavenBranchProjectDependencyVersionChecker(Path lastSavedPath, String ownerName, String artifactId, String targetArtifactId, String targetBranch) throws IOException {
		super(lastSavedPath, ownerName, artifactId, targetArtifactId);

		this.targetBranch = targetBranch;
	}

	@Override
	@NotNull
	protected String getTargetBranchName() {
		return targetBranch;
	}
}
