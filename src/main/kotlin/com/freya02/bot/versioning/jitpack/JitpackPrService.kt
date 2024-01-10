package com.freya02.bot.versioning.jitpack

import com.freya02.bot.config.PullUpdaterConfig
import com.freya02.bot.versioning.LibraryType
import com.freya02.bot.versioning.github.GithubBranch
import com.freya02.bot.versioning.github.PullRequest
import com.freya02.bot.versioning.github.PullRequestCache
import com.freya02.bot.versioning.jitpack.pullupdater.PullUpdateException
import com.freya02.bot.versioning.jitpack.pullupdater.PullUpdater
import dev.minn.jda.ktx.messages.send
import io.github.freya022.botcommands.api.core.service.annotations.BService
import io.github.oshai.kotlinlogging.KotlinLogging
import net.dv8tion.jda.api.interactions.InteractionHook

private val logger = KotlinLogging.logger { }

@BService
class JitpackPrService(private val pullUpdaterConfig: PullUpdaterConfig) {
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

    suspend fun updatePr(libraryType: LibraryType, pullNumber: Int, hook: InteractionHook, waitMessageId: Long, block: suspend (branch: GithubBranch) -> Unit) {
        val result = PullUpdater.tryUpdate(libraryType, pullNumber)
        hook.deleteMessageById(waitMessageId).queue()

        result.onSuccess {
            block(it)
        }.onFailure { exception ->
            if (exception is PullUpdateException && exception.type == PullUpdateException.ExceptionType.PR_UPDATE_FAILURE) {
                hook.send("Could not update pull request as it has merge conflicts", ephemeral = true).queue()
            } else {
                logger.catching(exception)
                hook.send("Could not update pull request", ephemeral = true).queue()
            }
        }
    }

    fun canUsePullUpdate(libraryType: LibraryType): Boolean {
        return libraryType == LibraryType.JDA && pullUpdaterConfig.enable
    }
}
