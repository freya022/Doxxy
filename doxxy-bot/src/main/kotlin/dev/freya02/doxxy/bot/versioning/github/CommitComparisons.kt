package dev.freya02.doxxy.bot.versioning.github

import dev.freya02.doxxy.bot.serializers.InstantSerializer
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

// While those are identical now, we still make an effort to separate GitHub's data,
// as the bot can update the PR and thus it should display data that differs from GitHub's
data class UpdatedCommitComparisons(
    val url: String,
    val aheadBy: Int,
    val behindBy: Int,
)

fun CommitComparisons.toUpdatedCommitComparisons() = UpdatedCommitComparisons(url = permalinkUrl, aheadBy = aheadBy, behindBy = behindBy)