package com.freya02.bot.utils

import net.dv8tion.jda.api.entities.Guild
import org.jetbrains.annotations.Contract
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.deleteIfExists
import kotlin.io.path.notExists
import kotlin.streams.asSequence

object Utils {
    @JvmStatic
    fun readResource(url: String): String {
        val callerClass = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE).callerClass
        try {
            requireNotNull(callerClass.getResourceAsStream(url)) { "Resource of class " + callerClass.simpleName + " at URL '" + url + "' does not exist" }
                .use { return it.readAllBytes().decodeToString() }
        } catch (e: IOException) {
            throw RuntimeException(
                "Unable to read resource of class " + callerClass.simpleName + " at URL '" + url + "'",
                e
            )
        }
    }

    fun getClassName(fullName: String): String {
        for ((index, c) in fullName.withIndex()) {
            if (c.isUpperCase()) {
                return fullName.substring(index)
            }
        }

        throw IllegalArgumentException("Could not get glass name from '$fullName'")
    }

    @Contract("null -> false")
    fun Guild?.isBCGuild(): Boolean {
        return when {
            this != null -> idLong == 848502702731165738L || idLong == 722891685755093072L
            else -> false
        }
    }

    fun Path.deleteRecursively() {
        if (this.notExists()) return

        Files.walk(this).use { stream ->
            stream
                .asSequence()
                .sortedDescending()
                .forEach { it.deleteIfExists() }
        }
    }

    inline fun <R> Path.withTemporaryFile(block: (Path) -> R) {
        try {
            block(this)
        } finally {
            this.deleteIfExists()
        }
    }
}