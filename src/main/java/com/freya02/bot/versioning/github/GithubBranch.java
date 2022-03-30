package com.freya02.bot.versioning.github;

import com.freya02.bot.versioning.ArtifactInfo;

public record GithubBranch(String ownerName, String repoName, String branchName, CommitHash latestCommitSha) {
	public ArtifactInfo toJitpackArtifact() {
		return new ArtifactInfo("com.github." + ownerName,
				repoName,
				latestCommitSha.asSha10());
	}
}
