package com.freya02.bot.versioning.github;

import gnu.trove.map.TIntObjectMap;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class PullRequestCache {
	private final String ownerName;
	private final String artifactId;
	private final String baseBranchName;

	private final UpdateCountdown updateCountdown = new UpdateCountdown(5, TimeUnit.MINUTES);
	private TIntObjectMap<PullRequest> pullRequests;

	public PullRequestCache(String ownerName, String artifactId, @Nullable String baseBranchName) {
		this.ownerName = ownerName;
		this.artifactId = artifactId;
		this.baseBranchName = baseBranchName;
	}

	public TIntObjectMap<PullRequest> getPullRequests() throws IOException {
		if (updateCountdown.needsUpdate()) {
			this.pullRequests = GithubUtils.retrievePullRequests(ownerName, artifactId, baseBranchName);
		}

		return pullRequests;
	}
}
