package dev.freya02.doxxy.github.client.data

import kotlinx.serialization.Serializable

@Serializable
data class Repository(
    val defaultBranch: String,
)