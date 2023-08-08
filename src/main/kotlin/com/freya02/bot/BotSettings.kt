package com.freya02.bot

import com.freya02.bot.utils.Utils.isBCGuild
import com.freya02.bot.utils.Utils.isJDAGuild
import com.freya02.botcommands.api.commands.CommandList
import com.freya02.botcommands.api.commands.CommandPath
import com.freya02.botcommands.api.core.SettingsProvider
import com.freya02.botcommands.api.core.service.annotations.BService
import net.dv8tion.jda.api.entities.Guild

@BService
object BotSettings : SettingsProvider {
    override fun getGuildCommands(guild: Guild): CommandList {
        return CommandList.filter { commandPath: CommandPath ->
            //If this is not a BC guild then we need to disable BC related subcommands in these guilds
            if (!guild.isBCGuild()) {
                if (commandPath.subname != null) {
                    return@filter commandPath.subname != "botcommands"
                }
            }

            //Disable tags for JDA
            if (guild.isJDAGuild() && commandPath.name.startsWith("tag")) {
                return@filter false
            }

            return@filter true
        }
    }
}
