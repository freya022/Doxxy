package dev.freya02.doxxy.common

import io.github.cdimascio.dotenv.dotenv
import kotlin.io.path.Path
import kotlin.io.path.absolute
import kotlin.io.path.notExists
import kotlin.io.path.pathString

object Env {

    private val env = dotenv {
        var envDir = Path(".").absolute()
        while (envDir.resolve(".env").notExists()) {
            envDir = envDir.parent
        }

        directory = envDir.pathString
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