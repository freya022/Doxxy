package com.freya02.bot.commands.text

import dev.minn.jda.ktx.coroutines.await
import io.github.freya022.botcommands.api.commands.annotations.Command
import io.github.freya022.botcommands.api.commands.text.BaseCommandEvent
import io.github.freya022.botcommands.api.commands.text.TextCommand
import io.github.freya022.botcommands.api.commands.text.annotations.JDATextCommandVariation
import io.github.freya022.botcommands.api.commands.text.annotations.RequireOwner
import io.github.freya022.botcommands.api.core.utils.awaitShutdown
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlin.system.exitProcess
import kotlin.time.Duration.Companion.seconds

@Command
class Exit : TextCommand() {
    private val logger = KotlinLogging.logger { }

    @RequireOwner
    @JDATextCommandVariation(path = ["exit"])
    suspend fun exit(event: BaseCommandEvent) {
        try {
            logger.warn { "Shutdown initiated by ${event.author.name} (${event.author.id})" }
            event.reactSuccess().mapToResult().await()

            event.jda.shutdown()
            event.jda.awaitShutdown(15.seconds)
        } catch (e: Exception) {
            exitProcess(2)
        } finally {
            exitProcess(0)
        }
    }
}