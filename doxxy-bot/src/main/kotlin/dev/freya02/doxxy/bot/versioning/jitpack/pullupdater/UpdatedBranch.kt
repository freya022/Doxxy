package dev.freya02.doxxy.bot.versioning.jitpack.pullupdater

import dev.freya02.doxxy.github.client.utils.CommitHash

data class UpdatedBranch(
    val ownerName: String,
    val repoName: String,
    val branchName: String,
    val sha: CommitHash,
)