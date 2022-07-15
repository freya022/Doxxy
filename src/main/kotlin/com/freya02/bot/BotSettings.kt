package com.freya02.bot

import com.freya02.bot.utils.Utils.isBCGuild
import com.freya02.botcommands.api.CommandList
import com.freya02.botcommands.api.SettingsProvider
import com.freya02.botcommands.api.application.CommandPath
import net.dv8tion.jda.api.entities.Guild

class BotSettings : SettingsProvider {
    override fun getGuildCommands(guild: Guild): CommandList {
        //If this is not a BC guild then we need to disable BC related subcommands in these guilds
        if (!guild.isBCGuild()) {
            return CommandList.filter { commandPath: CommandPath ->
                if (commandPath.subname != null) {
                    return@filter commandPath.subname != "botcommands"
                }

                return@filter true
            }
        }

        return super.getGuildCommands(guild)
    }
}