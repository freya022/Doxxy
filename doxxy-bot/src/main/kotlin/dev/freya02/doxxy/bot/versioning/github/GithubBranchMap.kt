package dev.freya02.doxxy.bot.versioning.github

@JvmRecord
data class GithubBranchMap(val defaultBranch: GithubBranch, val branches: Map<String, GithubBranch>)