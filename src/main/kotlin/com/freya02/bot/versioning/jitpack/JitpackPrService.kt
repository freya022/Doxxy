package com.freya02.bot.versioning.jitpack

import com.freya02.bot.config.PullUpdaterConfig
import com.freya02.bot.versioning.LibraryType
import com.freya02.bot.versioning.github.GithubBranch
import com.freya02.bot.versioning.github.GithubUtils
import com.freya02.bot.versioning.github.PullRequest
import com.freya02.bot.versioning.github.PullRequestCache
import com.freya02.bot.versioning.jitpack.jdafork.JDAFork
import com.freya02.bot.versioning.jitpack.jdafork.JDAForkException
import dev.minn.jda.ktx.coroutines.await
import dev.minn.jda.ktx.messages.send
import io.github.freya022.botcommands.api.components.event.ButtonEvent
import io.github.freya022.botcommands.api.core.service.annotations.BService
import io.github.oshai.kotlinlogging.KotlinLogging

@BService
class JitpackPrService(private val pullUpdaterConfig: PullUpdaterConfig) {
    data class PullUpdaterBranch(val forkBotName: String, val forkRepoName: String, val forkedBranchName: String) {
        fun toGithubBranch(): GithubBranch = GithubUtils.getBranch(forkBotName, forkRepoName, forkedBranchName)
    }

    private val logger = KotlinLogging.logger { }

    private val bcPullRequestCache = PullRequestCache("freya022", "BotCommands", null)
    private val jdaPullRequestCache = PullRequestCache("discord-jda", "JDA", "master")
    private val jdaKtxPullRequestCache = PullRequestCache("MinnDevelopment", "jda-ktx", "master")
    private val lavaPlayerPullRequestCache = PullRequestCache("Walkyst", "lavaplayer-fork", "original")

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

    suspend fun updatePr(event: ButtonEvent, pullNumber: Int, block: suspend (branch: PullUpdaterBranch) -> Unit) {
        //TODO prob move the reply part to the caller, pass only the hook + waiting message id
        event.deferEdit().queue()
        val waitMessage = when {
            JDAFork.isRunning -> "Please wait while the pull request is being updated, this may be longer than usual"
            else -> "Please wait while the pull request is being updated"
        }.let { event.hook.send(it, ephemeral = true).await() }

        val result = JDAFork.requestUpdate("JDA", pullNumber)
        event.hook.deleteMessageById(waitMessage.idLong).queue()

        if (result.isSuccess) {
            //TODO replace
            block(result.getOrThrow().let { PullUpdaterBranch(it.forkBotName, it.forkRepoName, it.forkedBranchName) })
        } else {
            val exception = result.exceptionOrNull() ?: throw IllegalStateException("Cannot have no exception")
            if (exception is JDAForkException && exception.type == JDAForkException.ExceptionType.PR_UPDATE_FAILURE) {
                event.hook.send("Could not update pull request as it has merge conflicts", ephemeral = true).queue()
            } else {
                logger.catching(exception)
                event.hook.send("Could not update pull request", ephemeral = true).queue()
            }
        }
    }

    fun canUsePullUpdate(libraryType: LibraryType): Boolean {
        return libraryType == LibraryType.JDA && pullUpdaterConfig.enable
    }
}
