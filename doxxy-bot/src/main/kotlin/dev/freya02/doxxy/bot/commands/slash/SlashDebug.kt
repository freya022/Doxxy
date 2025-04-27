package dev.freya02.doxxy.bot.commands.slash

import dev.freya02.doxxy.bot.utils.exceptions.DebugException
import dev.minn.jda.ktx.messages.reply_
import io.github.freya022.botcommands.api.commands.annotations.Command
import io.github.freya022.botcommands.api.commands.application.ApplicationCommand
import io.github.freya022.botcommands.api.commands.application.CommandScope
import io.github.freya022.botcommands.api.commands.application.annotations.Test
import io.github.freya022.botcommands.api.commands.application.slash.GuildSlashEvent
import io.github.freya022.botcommands.api.commands.application.slash.annotations.JDASlashCommand
import io.github.freya022.botcommands.api.commands.application.slash.annotations.TopLevelSlashCommandData

@Command
class SlashDebug : ApplicationCommand() {
    @Test
    @TopLevelSlashCommandData(scope = CommandScope.GUILD, defaultLocked = true)
    @JDASlashCommand(name = "debug", description = "Enables debugging")
    fun onSlashDebug(event: GuildSlashEvent) {
        try {
            throw DebugException()
        } catch (e: DebugException) {
            event.reply_("Debug enabled", ephemeral = true).queue()
        }
    }
}