package com.freya02.bot.commands.slash

import com.freya02.botcommands.api.application.ApplicationCommand
import com.freya02.botcommands.api.application.annotations.Test
import com.freya02.botcommands.api.application.slash.GuildSlashEvent
import com.freya02.botcommands.api.application.slash.annotations.JDASlashCommand

class SlashInvite : ApplicationCommand() {
    @Test
    @JDASlashCommand(name = "invite")
    fun onSlashInvite(event: GuildSlashEvent) {
        event.reply(event.jda.getInviteUrl()).queue()
    }
}