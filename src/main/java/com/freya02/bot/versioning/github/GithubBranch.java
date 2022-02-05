package com.freya02.bot.versioning.github;

public record GithubBranch(String branchName, String latestCommitSha) {
	public String latestCommitSha10() {
		return latestCommitSha().substring(0, 10);
	}
}
