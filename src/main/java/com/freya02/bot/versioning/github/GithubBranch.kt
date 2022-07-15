package com.freya02.bot.versioning.github

import com.freya02.bot.versioning.ArtifactInfo

@JvmRecord
data class GithubBranch(
    val ownerName: String,
    val repoName: String,
    val branchName: String,
    val latestCommitSha: CommitHash
) {
    val asJitpackArtifact: ArtifactInfo
        get() = ArtifactInfo(
            "com.github.$ownerName",
            repoName,
            latestCommitSha.asSha10()
        )

    val asURL: String
        get() = "https://github.com/$ownerName/$repoName/tree/$branchName"
}