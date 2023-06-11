package com.freya02.bot.versioning.jitpack

import com.freya02.bot.Config
import com.freya02.bot.utils.HttpUtils
import com.freya02.bot.versioning.LibraryType
import com.freya02.bot.versioning.github.GithubBranch
import com.freya02.bot.versioning.github.GithubUtils
import com.freya02.bot.versioning.github.PullRequest
import com.freya02.bot.versioning.github.PullRequestCache
import com.freya02.botcommands.api.components.event.ButtonEvent
import com.freya02.botcommands.api.core.service.annotations.BService
import dev.minn.jda.ktx.coroutines.await
import dev.minn.jda.ktx.messages.send
import dev.minn.jda.ktx.util.await
import mu.KotlinLogging
import net.dv8tion.jda.api.utils.data.DataObject
import okhttp3.Request

@BService
class JitpackPrService(private val config: Config) {
    data class PullUpdaterBranch(val forkBotName: String, val forkRepoName: String, val forkedBranchName: String) {
        fun toGithubBranch(): GithubBranch = GithubUtils.getBranch(forkBotName, forkRepoName, forkedBranchName)
    }

    private val logger = KotlinLogging.logger { }

    private val bcPullRequestCache = PullRequestCache("freya022", "BotCommands", null)
    private val jdaPullRequestCache = PullRequestCache("DV8FromTheWorld", "JDA", "master")
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
        event.deferEdit().queue()
        val waitMessage = event.hook.send("Please wait while the pull request is being updated", ephemeral = true).await()

        val response = HttpUtils.CLIENT.newCall(pullUpdateRequestBuilder("/update/JDA/$pullNumber").build()).await()
        event.hook.deleteMessageById(waitMessage.idLong).queue()

        val responseBody = response.body!!.string()
        if (response.isSuccessful) {
            val dataObject = DataObject.fromJson(responseBody)
            block(PullUpdaterBranch(
                dataObject.getString("forkBotName"),
                dataObject.getString("forkRepoName"),
                dataObject.getString("forkedBranchName")
            ))
        } else {
            val message = if (responseBody.isNotBlank()) DataObject.fromJson(responseBody).getString("message") else null
            if (response.code != 409)
                logger.warn("Could not update pull request, code: ${response.code}, response: $message")

            val userReply = when (response.code) {
                409 -> "Could not update pull request as it has merge conflicts"
                else -> "Could not update pull request"
            }
            event.hook.send(userReply, ephemeral = true).queue()
        }
    }

    private fun pullUpdateRequestBuilder(route: String) = Request.Builder()
        .url("${config.pullUpdaterBaseUrl}$route")
        .header("Authorization", "Bearer ${config.pullUpdaterToken}")
}
