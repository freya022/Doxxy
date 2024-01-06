package com.freya02.bot.versioning.github

import com.freya02.bot.versioning.LibraryType
import com.freya02.bot.versioning.github.GithubUtils.retrievePullRequests
import gnu.trove.map.TIntObjectMap
import kotlin.time.Duration.Companion.minutes

class PullRequestCache(
    private val libraryType: LibraryType,
    private val baseBranchName: String?
) {
    val pullRequests: TIntObjectMap<PullRequest> by UpdateCountdownDelegate(1.minutes) {
        retrievePullRequests(libraryType.githubOwnerName, libraryType.githubRepoName, baseBranchName)
    }
}