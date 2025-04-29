package dev.freya02.doxxy.bot.versioning.github

@JvmInline
value class CommitHash(val hash: String) {
    val asSha10: String
        get() = hash.substring(0, 10)
}