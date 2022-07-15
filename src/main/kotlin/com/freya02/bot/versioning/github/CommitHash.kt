package com.freya02.bot.versioning.github

@JvmRecord
data class CommitHash(val hash: String) {
    fun asSha10(): String {
        return hash.substring(0, 10)
    }
}