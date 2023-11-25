package com.freya02.bot.utils

import com.freya02.bot.config.Config
import io.github.freya022.botcommands.api.core.Logging
import net.dv8tion.jda.api.entities.Guild
import org.jetbrains.annotations.Contract
import java.nio.file.Path
import kotlin.io.path.deleteIfExists
import kotlin.math.log10
import kotlin.text.Typography.ellipsis

object Utils {
    const val bcGuildId: Long = 848502702731165738
    const val jdaGuildId: Long = 125227483518861312

    @Contract("null -> false")
    fun Guild?.isBCGuild(): Boolean = this?.idLong == bcGuildId || this?.idLong == Config.instance.fakeBCGuildId

    @Contract("null -> false")
    fun Guild?.isJDAGuild(): Boolean = this?.idLong == jdaGuildId || this?.idLong == Config.instance.fakeJDAGuildId

    inline fun <R> Path.withTemporaryFile(block: (Path) -> R) {
        try {
            block(this)
        } finally {
            this.deleteIfExists()
        }
    }

    inline fun <R> measureTime(desc: String, block: () -> R): R {
        val start = System.nanoTime()
        val r = block()
        val diff = System.nanoTime() - start

        //Abusing the fact that this method call with expose the real caller class
        Logging.currentLogger().info { "$desc took ${diff / 1000000.0} ms" }

        return r
    }

    inline fun <T> T.letIf(condition: Boolean, block: (T) -> T): T = when {
        condition -> block(this)
        else -> this
    }

    val Number.digitAmount: Int
        get() = 1 + log10(this.toDouble()).toInt()

    fun String.truncate(length: Int): String = when {
        this.length > length -> this.take(length - 1) + ellipsis
        else -> this
    }
}
