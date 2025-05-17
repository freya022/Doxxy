package dev.freya02.doxxy.github.client.data

import kotlinx.serialization.Serializable

@Serializable
data class Repo(
    val name: String,
    val owner: User,
)