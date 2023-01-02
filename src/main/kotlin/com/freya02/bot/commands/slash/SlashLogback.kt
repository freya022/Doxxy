package com.freya02.bot.commands.slash

import com.freya02.bot.utils.Utils
import com.freya02.bot.versioning.LibraryType
import com.freya02.botcommands.api.annotations.CommandMarker
import com.freya02.botcommands.api.commands.application.ApplicationCommand
import com.freya02.botcommands.api.commands.application.slash.GuildSlashEvent
import com.freya02.botcommands.api.commands.application.slash.annotations.JDASlashCommand
import dev.minn.jda.ktx.messages.MessageCreate

@CommandMarker
class SlashLogback : ApplicationCommand() {
    private val contentTemplate = """
        ```xml
        %s```
        Your `logback.xml` should go in the `src/main/resources` folder of your project.
    """.trimIndent()

    // BC/JDA wiki link depending on the guild
    @JDASlashCommand(name = "logback", description = "Gives a logback.xml")
    fun onSlashLogback(event: GuildSlashEvent) {
        val libraryType = LibraryType.getDefaultLibrary(event.guild)

        val message = MessageCreate {
            val logbackXml = when (libraryType) {
                LibraryType.JDA5 -> Utils.readResource("/logback_configs/JDA.xml")
                LibraryType.BOT_COMMANDS -> Utils.readResource("/logback_configs/BotCommands.xml")
                else -> throw IllegalArgumentException("Unexpected LibraryType: $libraryType")
            }

            content = contentTemplate.format(logbackXml)
        }

        event.reply(message).setEphemeral(true).queue()
    }
}