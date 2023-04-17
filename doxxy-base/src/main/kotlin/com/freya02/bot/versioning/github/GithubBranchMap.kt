package com.freya02.bot.versioning.github

@JvmRecord
data class GithubBranchMap(val defaultBranch: GithubBranch, val branches: Map<String, GithubBranch>)