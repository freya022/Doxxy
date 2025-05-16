package dev.freya02.doxxy.bot.versioning.jitpack

import dev.freya02.doxxy.bot.config.PullUpdaterConfig
import dev.freya02.doxxy.bot.versioning.LibraryType
import dev.freya02.doxxy.bot.versioning.github.CommitComparisons
import dev.freya02.doxxy.bot.versioning.github.GithubClient
import dev.freya02.doxxy.bot.versioning.github.PullRequest
import dev.freya02.doxxy.bot.versioning.github.PullRequestCache
import dev.freya02.doxxy.bot.versioning.jitpack.JitpackPrService.UpdatedCommitComparisons
import dev.freya02.doxxy.bot.versioning.jitpack.pullupdater.PullUpdateException
import dev.freya02.doxxy.bot.versioning.jitpack.pullupdater.PullUpdater
import dev.freya02.doxxy.bot.versioning.jitpack.pullupdater.UpdatedBranch
import io.github.freya022.botcommands.api.core.service.annotations.BService
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds

private val logger = KotlinLogging.logger { }

@BService
class JitpackPrService(
    private val pullUpdaterConfig: PullUpdaterConfig,
    private val pullUpdater: PullUpdater,
    private val client: GithubClient,
) {
    private val bcPullRequestCache = PullRequestCache(LibraryType.BOT_COMMANDS, null)
    private val jdaPullRequestCache = PullRequestCache(LibraryType.JDA, "master")
    private val jdaKtxPullRequestCache = PullRequestCache(LibraryType.JDA_KTX, "master")
    private val lavaPlayerPullRequestCache = PullRequestCache(LibraryType.LAVA_PLAYER, "main")

    fun getPullRequest(libraryType: LibraryType, pullNumber: Int): PullRequest? = when (libraryType) {
        LibraryType.BOT_COMMANDS -> bcPullRequestCache.pullRequests[pullNumber]
        LibraryType.JDA -> jdaPullRequestCache.pullRequests[pullNumber]
        LibraryType.JDA_KTX -> jdaKtxPullRequestCache.pullRequests[pullNumber]
        LibraryType.LAVA_PLAYER -> lavaPlayerPullRequestCache.pullRequests[pullNumber]
        else -> throw IllegalArgumentException()
    }

    fun getPullRequests(libraryType: LibraryType): Collection<PullRequest> = when (libraryType) {
        LibraryType.BOT_COMMANDS -> bcPullRequestCache.pullRequests.valueCollection()
        LibraryType.JDA -> jdaPullRequestCache.pullRequests.valueCollection()
        LibraryType.JDA_KTX -> jdaKtxPullRequestCache.pullRequests.valueCollection()
        LibraryType.LAVA_PLAYER -> lavaPlayerPullRequestCache.pullRequests.valueCollection()
        else -> throw IllegalArgumentException()
    }

    suspend fun updatePr(
        libraryType: LibraryType,
        pullNumber: Int,
        onMergeConflict: suspend () -> Unit,
        onError: suspend () -> Unit,
        onSuccess: suspend (branch: UpdatedBranch) -> Unit,
    ) {
        val result = pullUpdater.tryUpdate(libraryType, pullNumber)

        result.onSuccess {
            onSuccess(it)
        }.onFailure { exception ->
            if (exception is PullUpdateException && exception.type == PullUpdateException.ExceptionType.PR_UPDATE_FAILURE) {
                onMergeConflict()
            } else {
                logger.catching(exception)
                onError()
            }
        }
    }

    fun canUsePullUpdate(libraryType: LibraryType): Boolean {
        return libraryType == LibraryType.JDA && pullUpdaterConfig.enable
    }

    suspend fun fetchAdditionalPRDetails(libraryType: LibraryType, pullRequest: PullRequest, onUpdate: suspend (AdditionalPullRequestDetails) -> Unit) {
        val initialDetails = coroutineScope {
            val updatedPR = async {
                client.getPullRequest(libraryType.githubOwnerName, libraryType.githubRepoName, pullRequest.number)
            }

            val commitComparisons = async {
                val base = pullRequest.base.label
                val head = pullRequest.head.label

                client.compareCommits(libraryType.githubOwnerName, libraryType.githubRepoName, base, head)
                    .toUpdatedCommitComparisons()
            }

            val reverseCommitComparisons = async {
                val base = pullRequest.base.label
                val head = pullRequest.head.label

                client.compareCommits(libraryType.githubOwnerName, libraryType.githubRepoName, head, base)
            }

            AdditionalPullRequestDetails(updatedPR.await(), commitComparisons.await(), reverseCommitComparisons.await())
        }

        onUpdate(initialDetails)

        // If GitHub is computing the mergeability, retry until it has it
        if (initialDetails.updatedPR.mergeable == null) {
            var updatedPR = initialDetails.updatedPR
            while (updatedPR.mergeable == null) {
                delay(2.seconds)
                updatedPR = client.getPullRequest(libraryType.githubOwnerName, libraryType.githubRepoName, pullRequest.number)
            }

            onUpdate(initialDetails.copy(updatedPR = updatedPR))
        }
    }

    data class AdditionalPullRequestDetails(
        val updatedPR: PullRequest,
        val commitComparisons: UpdatedCommitComparisons,
        val reverseCommitComparisons: CommitComparisons,
    )

    data class UpdatedCommitComparisons(
        val url: String,
        val aheadBy: Int,
        val behindBy: Int,
    )
}

fun CommitComparisons.toUpdatedCommitComparisons() = UpdatedCommitComparisons(url = permalinkUrl, aheadBy = aheadBy, behindBy = behindBy)
