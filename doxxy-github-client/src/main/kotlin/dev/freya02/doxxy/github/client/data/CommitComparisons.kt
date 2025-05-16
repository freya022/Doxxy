package dev.freya02.doxxy.github.client.data

import dev.freya02.doxxy.github.client.serializers.InstantSerializer
import dev.freya02.doxxy.github.client.utils.CommitHash
import kotlinx.serialization.Serializable
import java.time.Instant

@Serializable
data class CommitComparisons(
    val permalinkUrl: String,
    val commits: List<Commit>,
    val aheadBy: Int,
    val behindBy: Int,
) {
    @Serializable
    data class Commit(
        val sha: CommitHash,
        val commit: Commit,
        val htmlUrl: String,
    ) {
        @Serializable
        data class Commit(
            val message: String,
            val committer: Committer
        ) {
            @Serializable
            data class Committer(
                @Serializable(with = InstantSerializer::class)
                val date: Instant,
            )
        }
    }
}
