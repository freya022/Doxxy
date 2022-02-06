package com.freya02.bot.versioning.maven;

import com.freya02.bot.versioning.ArtifactInfo;
import com.freya02.bot.versioning.VersionChecker;

import java.io.IOException;
import java.nio.file.Path;

public class MavenVersionChecker extends VersionChecker {
	private final RepoType repoType;

	private final String groupId;
	private final String artifactId;

	public MavenVersionChecker(Path lastSavedPath, RepoType repoType, String groupId, String artifactId) throws IOException {
		super(lastSavedPath);

		this.repoType = repoType;
		this.groupId = groupId;
		this.artifactId = artifactId;
	}

	@Override
	public boolean checkVersion() throws IOException {
		final ArtifactInfo latestVersion = new ArtifactInfo(groupId,
				artifactId,
				MavenUtils.getLatestMavenVersion(repoType.getUrlFormat(), groupId, artifactId));

		final boolean changed = !latestVersion.equals(this.latest);

		this.latest = latestVersion;

		return changed;
	}
}
