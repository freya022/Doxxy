package com.freya02.bot.versioning.github

@JvmInline
value class CommitHash(val hash: String) {
    val asSha10: String
        get() = hash.substring(0, 10)
}