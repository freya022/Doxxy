package com.freya02.bot.versioning.github;

public record GithubBranch(String branchName, CommitHash latestCommitSha) {
}
