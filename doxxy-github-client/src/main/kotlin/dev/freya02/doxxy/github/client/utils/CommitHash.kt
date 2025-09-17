package dev.freya02.doxxy.github.client.utils

import kotlinx.serialization.Serializable

@JvmInline
@Serializable
value class CommitHash(val hash: String) {
    val asSha10: String
        get() = hash.take(10)

    val asSha7: String
        get() = hash.take(7)
}
