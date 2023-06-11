package com.freya02.bot.commands.slash

import com.freya02.botcommands.api.commands.annotations.Command
import com.freya02.botcommands.api.commands.application.ApplicationCommand
import com.freya02.botcommands.api.commands.application.CommandScope
import com.freya02.botcommands.api.commands.application.annotations.Test
import com.freya02.botcommands.api.commands.application.slash.GuildSlashEvent
import com.freya02.botcommands.api.commands.application.slash.annotations.JDASlashCommand

@Command
class SlashInvite : ApplicationCommand() {
    @Test
    @JDASlashCommand(scope = CommandScope.GUILD, name = "invite")
    fun onSlashInvite(event: GuildSlashEvent) {
        event.reply(event.jda.getInviteUrl()).queue()
    }
}