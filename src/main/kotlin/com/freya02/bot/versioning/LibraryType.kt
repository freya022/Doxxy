package com.freya02.bot.versioning

import com.freya02.bot.utils.Utils.isBCGuild
import net.dv8tion.jda.api.entities.Guild

enum class LibraryType(val displayString: String, val githubOwnerName: String, val githubRepoName: String) {
    JDA("JDA", "discord-jda", "JDA"),
    JDA_KTX("JDA-KTX", "MinnDevelopment", "jda-ktx"),
    BOT_COMMANDS("BotCommands", "freya022", "BotCommands"),
    LAVA_PLAYER("LavaPlayer", "lavalink-devs", "lavaplayer"),
    ;

    companion object {
        fun getDefaultLibrary(guild: Guild): LibraryType {
            return if (guild.isBCGuild()) BOT_COMMANDS else JDA
        }
    }
}
