package com.freya02.bot.versioning

import com.freya02.bot.utils.Utils.isBCGuild
import net.dv8tion.jda.api.interactions.Interaction

enum class LibraryType(val displayString: String) {
    JDA4("JDA 4"),
    JDA5("JDA 5"),
    JDA_KTX("JDA-KTX"),
    BOT_COMMANDS("BotCommands");

    companion object {
        fun getDefaultLibrary(interaction: Interaction): LibraryType {
            return if (interaction.guild.isBCGuild()) BOT_COMMANDS else JDA5
        }
    }
}