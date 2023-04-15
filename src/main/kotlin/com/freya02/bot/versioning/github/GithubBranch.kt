package com.freya02.bot.versioning.github

import com.freya02.bot.versioning.ArtifactInfo

@JvmRecord
data class GithubBranch(
    val ownerName: String,
    val authorName: String,
    val repoName: String,
    val branchName: String,
    val latestCommitSha: CommitHash
) {
    fun toJitpackArtifact(): ArtifactInfo = ArtifactInfo(
        "io.github.$ownerName",
        repoName,
        latestCommitSha.asSha10
    )

    fun toURL(): String = "https://github.com/$ownerName/$repoName/tree/$branchName"
}