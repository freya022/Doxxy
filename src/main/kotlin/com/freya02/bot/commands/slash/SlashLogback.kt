package com.freya02.bot.commands.slash

import com.freya02.bot.commands.slash.DeleteButtonListener.Companion.messageDeleteButton
import com.freya02.bot.versioning.LibraryType
import com.freya02.botcommands.api.commands.annotations.Command
import com.freya02.botcommands.api.commands.application.ApplicationCommand
import com.freya02.botcommands.api.commands.application.slash.GuildSlashEvent
import com.freya02.botcommands.api.commands.application.slash.annotations.JDASlashCommand
import com.freya02.botcommands.api.commands.application.slash.annotations.SlashOption
import com.freya02.botcommands.api.components.Components
import com.freya02.botcommands.api.core.utils.readResourceAsString
import dev.minn.jda.ktx.interactions.components.row
import dev.minn.jda.ktx.messages.MessageCreate
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback

@Command
class SlashLogback(private val componentsService: Components) : ApplicationCommand() {
    private val contentTemplate = """
        ```xml
        %s```
        Your `logback.xml` should go in the `src/main/resources` folder of your project.
        
        More info: %s
    """.trimIndent()

    @JDASlashCommand(name = "logback", description = "Gives a logback.xml")
    fun onSlashLogback(
        event: GuildSlashEvent,
        @SlashOption(description = "Whether to display the reply as an ephemeral message") ephemeral: Boolean?
    ) = when (ephemeral) {
        null -> onLogbackRequest(event)
        else -> onLogbackRequest(event, ephemeral)
    }

    fun onLogbackRequest(event: IReplyCallback, ephemeral: Boolean = true) {
        // BC/JDA wiki link depending on the guild
        val libraryType = LibraryType.getDefaultLibrary(event.guild!!)

        val message = MessageCreate {
            val logbackXml = when (libraryType) {
                LibraryType.JDA -> readResourceAsString("/logback_configs/JDA.xml")
                LibraryType.BOT_COMMANDS -> readResourceAsString("/logback_configs/BotCommands.xml")
                else -> throw IllegalArgumentException("Unexpected LibraryType: $libraryType")
            }

            val wikiLink = when (libraryType) {
                LibraryType.JDA -> "https://jda.wiki/setup/logging/"
                LibraryType.BOT_COMMANDS -> "https://freya022.github.io/BotCommands-Wiki/Logging/"
                else -> throw IllegalArgumentException("Unexpected LibraryType: $libraryType")
            }

            content = contentTemplate.format(logbackXml, wikiLink)

            if (!ephemeral) {
                components += row(componentsService.messageDeleteButton(event.user))
            }
        }

        event.reply(message).setEphemeral(ephemeral).queue()
    }
}
