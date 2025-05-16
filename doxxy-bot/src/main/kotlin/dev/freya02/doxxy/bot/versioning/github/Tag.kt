package dev.freya02.doxxy.bot.versioning.github

import kotlinx.serialization.Serializable

@Serializable
data class Tag(val name: String, val commit: Commit) {

    @Serializable
    data class Commit(val sha: CommitHash)
}
