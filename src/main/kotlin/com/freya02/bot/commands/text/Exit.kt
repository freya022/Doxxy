package com.freya02.bot.commands.text

import com.freya02.botcommands.api.commands.annotations.Command
import com.freya02.botcommands.api.commands.annotations.RequireOwner
import com.freya02.botcommands.api.commands.prefixed.BaseCommandEvent
import com.freya02.botcommands.api.commands.prefixed.TextCommand
import com.freya02.botcommands.api.commands.prefixed.annotations.JDATextCommand
import mu.KotlinLogging
import net.dv8tion.jda.api.JDA
import kotlin.system.exitProcess

@Command
class Exit : TextCommand() {
    private val logger = KotlinLogging.logger { }

    @RequireOwner
    @JDATextCommand(name = "exit")
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