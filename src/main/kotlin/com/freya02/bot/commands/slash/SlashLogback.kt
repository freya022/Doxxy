package com.freya02.bot.commands.slash

import com.freya02.bot.commands.slash.DeleteButtonListener.Companion.messageDeleteButton
import com.freya02.bot.logback.LogbackProfile
import com.freya02.bot.versioning.LibraryType
import dev.minn.jda.ktx.interactions.components.row
import dev.minn.jda.ktx.messages.MessageCreate
import io.github.freya022.botcommands.api.commands.annotations.Command
import io.github.freya022.botcommands.api.commands.application.ApplicationCommand
import io.github.freya022.botcommands.api.commands.application.slash.GuildSlashEvent
import io.github.freya022.botcommands.api.commands.application.slash.annotations.JDASlashCommand
import io.github.freya022.botcommands.api.commands.application.slash.annotations.SlashOption
import io.github.freya022.botcommands.api.components.Components
import io.github.freya022.botcommands.api.core.utils.readResourceAsString
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback

private const val ephemeralDefault = true
private val profileDefault = LogbackProfile.DEV

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
        @SlashOption(description = "Default: true, reply as an ephemeral message?") ephemeral: Boolean = ephemeralDefault,
        @SlashOption(description = "Default: Dev, whether to display the reply as an ephemeral message", usePredefinedChoices = true) profile: LogbackProfile = profileDefault
    ) = onLogbackRequest(event, ephemeral)

    fun onLogbackRequest(event: IReplyCallback, ephemeral: Boolean = ephemeralDefault, profile: LogbackProfile = profileDefault) {
        // BC/JDA wiki link depending on the guild
        val libraryType = LibraryType.getDefaultLibrary(event.guild!!)

        val message = MessageCreate {
            val basePath = "/logback_configs/${profile.pathFragment}"
            val logbackXml = when (libraryType) {
                LibraryType.JDA -> readResourceAsString("$basePath/JDA.xml")
                LibraryType.BOT_COMMANDS -> readResourceAsString("$basePath/BotCommands.xml")
                else -> throw IllegalArgumentException("Unexpected LibraryType: $libraryType")
            }

            val wikiLink = when (libraryType) {
                LibraryType.JDA -> "https://jda.wiki/setup/logging/"
                LibraryType.BOT_COMMANDS -> "https://freya022.github.io/BotCommands/3.X/setup/logging/"
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
