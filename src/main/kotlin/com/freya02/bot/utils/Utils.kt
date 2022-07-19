package com.freya02.bot.utils

import com.freya02.botcommands.api.Logging
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.internal.utils.JDALogger
import org.jetbrains.annotations.Contract
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.sql.Statement
import kotlin.io.path.deleteIfExists
import kotlin.io.path.notExists
import kotlin.streams.asSequence

object Utils {
    val walker: StackWalker = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE)

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

    fun Statement.toSQLString() = toString().substringAfter(" wrapping ")
        .lines()
        .joinToString(" ") { it.trim() }

    @Suppress("NOTHING_TO_INLINE")
    inline fun Statement.logQuery() = logQuery(walker.callerClass)

    fun Statement.logQuery(callerClass: Class<*>) {
        JDALogger.getLog(callerClass).trace("Running query '${this.toSQLString()}'")
    }

    inline fun <R> measureTime(desc: String, block: () -> R): R {
        val start = System.nanoTime()
        val r = block()
        val diff = System.nanoTime() - start

        //Abusing the fact that this method call with expose the real caller class
        Logging.getLogger().debug("$desc took ${diff / 1000000.0} ms")

        return r
    }
}