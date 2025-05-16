package dev.freya02.doxxy.bot.versioning.github

import dev.freya02.doxxy.bot.versioning.ArtifactInfo

@JvmRecord
data class GithubBranch(
    val ownerName: String,
    val repoName: String,
    val branchName: String,
    val latestCommitSha: CommitHash
)

val GithubBranch.jitpackArtifact: ArtifactInfo get() = ArtifactInfo(
    "io.github.$ownerName",
    repoName,
    latestCommitSha.asSha10
)

val GithubBranch.url: String get() = "https://github.com/$ownerName/$repoName/tree/$branchName"
