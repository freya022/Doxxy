package com.freya02.bot.commands.slash

import io.github.freya022.botcommands.api.commands.annotations.Command
import io.github.freya022.botcommands.api.commands.application.ApplicationCommand
import io.github.freya022.botcommands.api.commands.application.CommandScope
import io.github.freya022.botcommands.api.commands.application.annotations.Test
import io.github.freya022.botcommands.api.commands.application.slash.GuildSlashEvent
import io.github.freya022.botcommands.api.commands.application.slash.annotations.JDASlashCommand
import io.github.freya022.botcommands.api.commands.application.slash.annotations.TopLevelSlashCommandData

@Command
class SlashInvite : ApplicationCommand() {
    @Test
    @TopLevelSlashCommandData(scope = CommandScope.GUILD)
    @JDASlashCommand(name = "invite")
    fun onSlashInvite(event: GuildSlashEvent) {
        event.reply(event.jda.getInviteUrl()).queue()
    }
}