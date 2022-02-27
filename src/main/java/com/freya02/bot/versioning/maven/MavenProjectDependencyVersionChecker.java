package com.freya02.bot.versioning.maven;

import com.freya02.bot.versioning.ArtifactInfo;
import com.freya02.bot.versioning.VersionChecker;
import com.freya02.bot.versioning.github.GithubUtils;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Path;

public class MavenProjectDependencyVersionChecker extends VersionChecker {
	private final String ownerName;
	private final String artifactId;

	private final String targetArtifactId;

	public MavenProjectDependencyVersionChecker(Path lastSavedPath, String ownerName, String artifactId, String targetArtifactId) throws IOException {
		super(lastSavedPath);

		this.ownerName = ownerName;
		this.artifactId = artifactId;

		this.targetArtifactId = targetArtifactId;
	}

	@Override
	public boolean checkVersion() throws IOException {
		final String targetBranchName = getTargetBranchName();

		final ArtifactInfo latestDependencyVersion = MavenUtils.retrieveDependencyVersion(ownerName, artifactId, targetBranchName, targetArtifactId);

		final boolean changed = !latestDependencyVersion.equals(this.diskLatest);

		this.latest = latestDependencyVersion;

		return changed;
	}

	@NotNull
	protected String getTargetBranchName() throws IOException {
		return GithubUtils.getLatestBranch(ownerName, artifactId).branchName();
	}
}
