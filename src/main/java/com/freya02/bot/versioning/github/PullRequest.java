package com.freya02.bot.versioning.github;

import com.freya02.bot.versioning.ArtifactInfo;
import net.dv8tion.jda.api.utils.data.DataObject;

public record PullRequest(int number, String title, boolean draft, GithubBranch branch, String pullUrl) {
	public static PullRequest fromData(DataObject data) {
		final DataObject head = data.getObject("head");

		if (head.isNull("repo")) { //If we don't have that then the head repo (the fork) doesn't exist
			return null;
		}

		final DataObject headRepo = head.getObject("repo");

		final int number = data.getInt("number");
		final String title = data.getString("title");
		final boolean draft = data.getBoolean("draft");
		final String headRepoOwnerName = data.getObject("user").getString("login");
		final String headRepoName = headRepo.getString("name");
		final String headBranchName = head.getString("ref");
		final String latestHash = head.getString("sha");
		final String pullUrl = data.getString("html_url");

		return new PullRequest(number, title, draft, new GithubBranch(headRepoOwnerName, headRepoName, headBranchName, new CommitHash(latestHash)), pullUrl);
	}

	public ArtifactInfo toJitpackArtifact() {
		return branch.toJitpackArtifact();
	}
}
