package com.freya02.bot.versioning.github

@JvmRecord
data class CommitHash(val hash: String) {
    val asSha10: String
        get() = hash.substring(0, 10)
}