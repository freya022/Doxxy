package dev.freya02.doxxy.github.client.data

import dev.freya02.doxxy.github.client.utils.CommitHash
import kotlinx.serialization.Serializable

@Serializable
data class Tag(val name: String, val commit: Commit) {

    @Serializable
    data class Commit(val sha: CommitHash)
}
