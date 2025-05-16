package dev.freya02.doxxy.github.client.data

import kotlinx.serialization.Serializable

@Serializable
data class User(
    val login: String,
)

