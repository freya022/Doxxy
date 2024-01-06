package com.freya02.bot.versioning.jitpack.pullupdater

data class GithubUser(val login: String) {
    val userName get() = login
}

data class GithubRepository(val name: String)

data class PullRequest(val head: Branch, val base: Branch, val merged: Boolean, val mergeable: Boolean?) {
    data class Branch(val label: String, val ref: String, val sha: String, val user: GithubUser, val repo: GithubRepository) {
        val branchName get() = ref
    }
}