package com.freya02.bot.commands.slash.versioning

import com.freya02.bot.commands.slash.DeleteButtonListener.Companion.messageDeleteButton
import com.freya02.bot.versioning.LibraryType
import com.freya02.bot.versioning.Versions
import com.freya02.botcommands.api.annotations.CommandMarker
import com.freya02.botcommands.api.commands.application.ApplicationCommand
import com.freya02.botcommands.api.commands.application.annotations.AppOption
import com.freya02.botcommands.api.commands.application.slash.GuildSlashEvent
import com.freya02.botcommands.api.commands.application.slash.annotations.JDASlashCommand
import com.freya02.botcommands.api.components.Components
import net.dv8tion.jda.api.EmbedBuilder

@CommandMarker
class SlashLatest(private val versions: Versions, private val components: Components) : ApplicationCommand() {
    @JDASlashCommand(name = "latest", description = "Shows the latest version of the library")
    fun onSlashLatest(
        event: GuildSlashEvent,
        @AppOption(description = "Type of library") libraryType: LibraryType?
    ) {
        val builder = EmbedBuilder().setTitle("Latest versions")

        when (libraryType) {
            null -> {
                builder.addBCVersion()
                builder.addBlankField(true)
                builder.addJDA5Version()
                builder.addJDAKtxVersion()
                builder.addLavaPlayerVersion()
            }
            LibraryType.BOT_COMMANDS -> builder.addBCVersion()
            LibraryType.JDA5 -> builder.addJDA5Version()
            LibraryType.JDA_KTX -> builder.addJDAKtxVersion()
            LibraryType.LAVA_PLAYER -> builder.addLavaPlayerVersion()
        }

        event.replyEmbeds(builder.build())
            .addActionRow(components.messageDeleteButton(event.user))
            .queue()
    }

    private fun EmbedBuilder.addJDA5Version() {
        addField("JDA 5", "`" + versions.latestJDA5Version.version + "`", true)
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