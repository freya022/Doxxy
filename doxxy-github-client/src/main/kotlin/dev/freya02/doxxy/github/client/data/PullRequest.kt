package dev.freya02.doxxy.github.client.data

import kotlinx.serialization.Serializable

@Serializable
data class PullRequest(
    val number: Int,
    val title: String,
    val draft: Boolean,
    val user: User,
    val base: Branch,
    val head: Branch,
    val mergeable: Boolean? = null,
    val htmlUrl: String,
) {

    val authorName get() = user.login
    val pullUrl get() = htmlUrl
    val branch get() = head
}