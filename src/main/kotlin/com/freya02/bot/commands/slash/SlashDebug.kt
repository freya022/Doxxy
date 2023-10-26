package com.freya02.bot.commands.slash

import com.freya02.bot.utils.exceptions.DebugException
import dev.minn.jda.ktx.messages.reply_
import io.github.freya022.botcommands.api.commands.annotations.Command
import io.github.freya022.botcommands.api.commands.application.ApplicationCommand
import io.github.freya022.botcommands.api.commands.application.CommandScope
import io.github.freya022.botcommands.api.commands.application.annotations.Test
import io.github.freya022.botcommands.api.commands.application.slash.GuildSlashEvent
import io.github.freya022.botcommands.api.commands.application.slash.annotations.JDASlashCommand

@Command
class SlashDebug : ApplicationCommand() {
    @Test
    @JDASlashCommand(
        scope = CommandScope.GUILD,
        name = "debug",
        description = "Enables debugging",
        defaultLocked = true
    )
    fun onSlashDebug(event: GuildSlashEvent) {
        try {
            throw DebugException()
        } catch (e: DebugException) {
            event.reply_("Debug enabled", ephemeral = true).queue()
        }
    }
}