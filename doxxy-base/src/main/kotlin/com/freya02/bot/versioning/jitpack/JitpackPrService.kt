package com.freya02.bot.versioning.jitpack

import com.freya02.bot.versioning.LibraryType
import com.freya02.bot.versioning.github.PullRequest
import com.freya02.bot.versioning.github.PullRequestCache

class JitpackPrService {
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
}
