package dev.freya02.doxxy.github.client.data

import dev.freya02.doxxy.github.client.utils.CommitHash
import kotlinx.serialization.Serializable

@Serializable
data class Branch(
    val label: String,
    val user: User,
    val repo: Repo,
    val ref: String,
    val sha: CommitHash,
) {

    val ownerName get() = user.login
    val repoName get() = repo.name
    val branchName get() = ref
    val latestCommitSha get() = sha
}
