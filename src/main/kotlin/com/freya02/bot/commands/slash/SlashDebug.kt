package com.freya02.bot.commands.slash

import com.freya02.bot.utils.exceptions.DebugException
import com.freya02.botcommands.api.annotations.CommandMarker
import com.freya02.botcommands.api.application.ApplicationCommand
import com.freya02.botcommands.api.application.CommandScope
import com.freya02.botcommands.api.application.annotations.Test
import com.freya02.botcommands.api.application.slash.GuildSlashEvent
import com.freya02.botcommands.api.application.slash.annotations.JDASlashCommand
import dev.minn.jda.ktx.messages.reply_

@CommandMarker
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