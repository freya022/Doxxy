package dev.freya02.doxxy.bot.versioning.github

import kotlinx.serialization.Serializable

interface Commits {

    @Serializable
    data class Commit(
        val sha: CommitHash,
    )
}