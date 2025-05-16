package dev.freya02.doxxy.bot.utils.github

import dev.freya02.doxxy.bot.utils.UpdateCountdown
import dev.freya02.doxxy.bot.versioning.LibraryType
import dev.freya02.doxxy.github.client.GithubClient
import dev.freya02.doxxy.github.client.data.PullRequest
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.toList
import kotlin.time.Duration.Companion.minutes

private val logger = KotlinLogging.logger { }

class PullRequestCache(
    private val githubClient: GithubClient,
    private val githubOwnerName: String,
    private val githubRepoName: String,
    private val baseBranchName: String?
) {
    constructor(
        githubClient: GithubClient,
        libraryType: LibraryType,
        baseBranchName: String?
    ) : this(githubClient, libraryType.githubOwnerName, libraryType.githubRepoName, baseBranchName)

    private val updateCountdown = UpdateCountdown(1.minutes)

    private lateinit var pullRequests: Map<Int, PullRequest>

    suspend fun retrievePullRequests(): Map<Int, PullRequest> {
        updateCountdown.onUpdate {
            logger.debug {
                val targetBranch = baseBranchName?.let { "'$it'" } ?: "the default branch"
                "Retrieving pull requests of '$githubOwnerName/$githubRepoName' targeting $targetBranch"
            }

            pullRequests = githubClient.getPullRequests(githubOwnerName, githubRepoName, baseBranchName ?: GithubClient.DEFAULT_BRANCH, perPage = 100)
                .toList()
                .associateBy { it.number }
        }

        return pullRequests
    }
}