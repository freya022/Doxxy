package dev.freya02.doxxy.common

import io.github.cdimascio.dotenv.dotenv

object Env {

    private val env = dotenv {

    }

    operator fun get(key: String): String = getOrNull(key)
        ?: throw IllegalArgumentException("Cannot find env key '$key'")

    fun getOrNull(key: String): String? =
        env[key]

    fun getList(name: String): List<String> = getList(name) { it }

    fun <R> getList(name: String, transform: (String) -> R): List<R> {
        return get(name).split(",").map { it.trim().let(transform) }
    }
}