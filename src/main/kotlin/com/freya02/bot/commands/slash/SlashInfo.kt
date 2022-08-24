package com.freya02.bot.commands.slash

import com.freya02.botcommands.api.annotations.CommandMarker
import com.freya02.botcommands.api.commands.application.ApplicationCommand
import com.freya02.botcommands.api.commands.application.CommandScope
import com.freya02.botcommands.api.commands.application.slash.GuildSlashEvent
import com.freya02.botcommands.api.commands.application.slash.annotations.JDASlashCommand
import dev.minn.jda.ktx.messages.Embed
import net.dv8tion.jda.api.JDAInfo

@CommandMarker
class SlashInfo : ApplicationCommand() {
    @JDASlashCommand(scope = CommandScope.GLOBAL_NO_DM, name = "info", description = "Gives info on the bot")
    fun onSlashInfo(event: GuildSlashEvent) {
        event.replyEmbeds(Embed {
            author {
                name = event.jda.selfUser.name
                iconUrl = event.jda.selfUser.effectiveAvatarUrl
            }

            field {
                name = "JDA version"
                value = "[${JDAInfo.VERSION}](https://github.com/DV8FromTheWorld/JDA)"
                inline = true
            }

            field {
                name = "Gateway version"
                value = JDAInfo.DISCORD_GATEWAY_VERSION.toString()
                inline = true
            }

            field {
                name = "REST version"
                value = JDAInfo.DISCORD_REST_VERSION.toString()
                inline = true
            }
        }).setEphemeral(true).queue()
    }
}