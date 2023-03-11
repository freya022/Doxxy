package com.freya02.bot.commands.slash

import com.freya02.bot.utils.Utils
import com.freya02.bot.versioning.LibraryType
import com.freya02.botcommands.api.annotations.CommandMarker
import com.freya02.botcommands.api.commands.application.ApplicationCommand
import com.freya02.botcommands.api.commands.application.slash.GuildSlashEvent
import com.freya02.botcommands.api.commands.application.slash.annotations.JDASlashCommand
import dev.minn.jda.ktx.messages.MessageCreate
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback

@CommandMarker
class SlashLogback : ApplicationCommand() {
    private val contentTemplate = """
        ```xml
        %s```
        Your `logback.xml` should go in the `src/main/resources` folder of your project.
        
        More info: %s
    """.trimIndent()

    @JDASlashCommand(name = "logback", description = "Gives a logback.xml")
    fun onSlashLogback(event: GuildSlashEvent) {
        onLogbackRequest(event)
    }

    fun onLogbackRequest(event: IReplyCallback) {
        // BC/JDA wiki link depending on the guild
        val libraryType = LibraryType.getDefaultLibrary(event.guild!!)

        val message = MessageCreate {
            val logbackXml = when (libraryType) {
                LibraryType.JDA -> Utils.readResource("/logback_configs/JDA.xml")
                LibraryType.BOT_COMMANDS -> Utils.readResource("/logback_configs/BotCommands.xml")
                else -> throw IllegalArgumentException("Unexpected LibraryType: $libraryType")
            }

            val wikiLink = when (libraryType) {
                LibraryType.JDA -> "https://jda.wiki/setup/logging/"
                LibraryType.BOT_COMMANDS -> "https://freya022.github.io/BotCommands-Wiki/Logging/"
                else -> throw IllegalArgumentException("Unexpected LibraryType: $libraryType")
            }

            content = contentTemplate.format(logbackXml, wikiLink)
        }

        event.reply(message).setEphemeral(true).queue()
    }
}
