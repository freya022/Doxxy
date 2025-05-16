package dev.freya02.doxxy.github.client.data

import dev.freya02.doxxy.github.client.utils.CommitHash
import kotlinx.serialization.Serializable

interface Commits {

    @Serializable
    data class Commit(
        val sha: CommitHash,
    )
}