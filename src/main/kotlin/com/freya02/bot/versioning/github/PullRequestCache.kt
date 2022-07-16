package com.freya02.bot.versioning.github

import com.freya02.bot.versioning.github.GithubUtils.retrievePullRequests
import gnu.trove.map.TIntObjectMap
import kotlin.time.Duration.Companion.minutes

class PullRequestCache(
    private val ownerName: String,
    private val artifactId: String,
    private val baseBranchName: String?
) {
    val pullRequests: TIntObjectMap<PullRequest> by UpdateCountdownDelegate(5.minutes) {
        retrievePullRequests(ownerName, artifactId, baseBranchName)
    }
}