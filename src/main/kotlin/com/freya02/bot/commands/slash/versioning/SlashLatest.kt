package com.freya02.bot.commands.slash.versioning

import com.freya02.bot.commands.slash.DeleteButtonListener.Companion.messageDeleteButton
import com.freya02.bot.utils.Utils.isBCGuild
import com.freya02.bot.utils.Utils.isJDAGuild
import com.freya02.bot.versioning.LibraryType
import com.freya02.bot.versioning.Versions
import com.freya02.botcommands.api.commands.annotations.Command
import com.freya02.botcommands.api.commands.application.ApplicationCommand
import com.freya02.botcommands.api.commands.application.annotations.AppOption
import com.freya02.botcommands.api.commands.application.slash.GuildSlashEvent
import com.freya02.botcommands.api.commands.application.slash.annotations.JDASlashCommand
import com.freya02.botcommands.api.components.Components
import net.dv8tion.jda.api.EmbedBuilder

@Command
class SlashLatest(private val versions: Versions, private val components: Components) : ApplicationCommand() {
    @JDASlashCommand(name = "latest", description = "Shows the latest version of the library")
    fun onSlashLatest(
        event: GuildSlashEvent,
        @AppOption(name = "library", description = "The target library", usePredefinedChoices = true) libraryType: LibraryType?
    ) {
        val builder = EmbedBuilder().setTitle("Latest versions")

        when (libraryType) {
            null -> {
                if (event.guild.isBCGuild()) {
                    builder.addBCVersion()
                    builder.addBlankField(true)
                }
                builder.addJDAVersion()
                builder.addJDAKtxVersion()
                when {
                    event.guild.isJDAGuild() -> builder.addLavaPlayerVersion()
                    else -> builder.addBlankField(true)
                }
            }
            LibraryType.BOT_COMMANDS -> builder.addBCVersion()
            LibraryType.JDA -> builder.addJDAVersion()
            LibraryType.JDA_KTX -> builder.addJDAKtxVersion()
            LibraryType.LAVA_PLAYER -> builder.addLavaPlayerVersion()
        }

        event.replyEmbeds(builder.build())
            .addActionRow(components.messageDeleteButton(event.user))
            .queue()
    }

    private fun EmbedBuilder.addJDAVersion() {
        addField("JDA", "`" + versions.latestJDAVersion.version + "`", true)
    }

    private fun EmbedBuilder.addJDAKtxVersion() {
        addField("JDA KTX", "`" + versions.latestJDAKtxVersion.version + "`", true)
    }

    private fun EmbedBuilder.addLavaPlayerVersion() {
        addField("LavaPlayer", "`" + versions.latestLavaPlayerVersion.version + "`", true)
    }

    private fun EmbedBuilder.addBCVersion() {
        addField("BotCommands version", "`" + versions.latestBotCommandsVersion.version + "`", true)
        addField("JDA version for BC", "`" + versions.jdaVersionFromBotCommands.version + "`", true)
    }
}
