package com.freya02.bot.versioning.jitpack;

import com.freya02.bot.versioning.ArtifactInfo;
import com.freya02.bot.versioning.VersionChecker;
import com.freya02.bot.versioning.github.GithubBranch;
import com.freya02.bot.versioning.github.GithubUtils;

import java.io.IOException;
import java.nio.file.Path;

public class JitpackVersionChecker extends VersionChecker {
	private final String ownerName;
	private final String groupId;
	private final String artifactId;

	private GithubBranch latestBranch;

	public JitpackVersionChecker(Path lastSavedPath, String ownerName, String groupId, String artifactId) throws IOException {
		super(lastSavedPath);

		this.ownerName = ownerName;
		this.groupId = groupId;
		this.artifactId = artifactId;
	}

	@Override
	public boolean checkVersion() throws IOException {
		this.latestBranch = GithubUtils.getLatestBranch(ownerName, artifactId);

		final ArtifactInfo latestVersion = new ArtifactInfo(groupId,
				artifactId,
				latestBranch.latestCommitSha10());

		final boolean changed = !latestVersion.equals(this.latest);

		this.latest = latestVersion;

		return changed;
	}

	public GithubBranch getLatestBranch() {
		return latestBranch;
	}
}
