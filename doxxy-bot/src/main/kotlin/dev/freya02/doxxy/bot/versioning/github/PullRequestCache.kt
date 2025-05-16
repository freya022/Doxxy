package dev.freya02.doxxy.bot.versioning.github

import dev.freya02.doxxy.bot.utils.UpdateCountdownDelegate
import dev.freya02.doxxy.bot.versioning.LibraryType
import dev.freya02.doxxy.bot.versioning.github.GithubUtils.retrievePullRequests
import gnu.trove.map.TIntObjectMap
import kotlin.time.Duration.Companion.minutes

class PullRequestCache(
    private val githubOwnerName: String,
    private val githubRepoName: String,
    private val baseBranchName: String?
) {
    constructor(
        libraryType: LibraryType,
        baseBranchName: String?
    ) : this(libraryType.githubOwnerName, libraryType.githubRepoName, baseBranchName)

    val pullRequests: TIntObjectMap<PullRequest> by UpdateCountdownDelegate(1.minutes) {
        retrievePullRequests(githubOwnerName, githubRepoName, baseBranchName)
    }
}