package com.freya02.bot.commands.text

import io.github.freya022.botcommands.api.commands.annotations.Command
import io.github.freya022.botcommands.api.commands.annotations.RequireOwner
import io.github.freya022.botcommands.api.commands.text.BaseCommandEvent
import io.github.freya022.botcommands.api.commands.text.TextCommand
import io.github.freya022.botcommands.api.commands.text.annotations.JDATextCommand
import mu.KotlinLogging
import net.dv8tion.jda.api.JDA
import kotlin.system.exitProcess

@Command
class Exit : TextCommand() {
    private val logger = KotlinLogging.logger { }

    @RequireOwner
    @JDATextCommand(path = ["exit"])
    fun exit(event: BaseCommandEvent) {
        logger.warn("Shutdown initiated by {} ({})", event.author.name, event.author.id)
        event.reactSuccess().mapToResult().complete()

        event.jda.shutdown()
        while (event.jda.status != JDA.Status.SHUTDOWN) {
            Thread.sleep(15)
            Thread.onSpinWait()
        }

        exitProcess(0)
    }
}