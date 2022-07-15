package com.freya02.bot.commands.slash.versioning

import com.freya02.bot.commands.slash.DeleteButtonListener.Companion.getDeleteButton
import com.freya02.bot.versioning.LibraryType
import com.freya02.bot.versioning.Versions
import com.freya02.botcommands.api.annotations.CommandMarker
import com.freya02.botcommands.api.application.ApplicationCommand
import com.freya02.botcommands.api.application.annotations.AppOption
import com.freya02.botcommands.api.application.slash.GuildSlashEvent
import com.freya02.botcommands.api.application.slash.annotations.JDASlashCommand
import net.dv8tion.jda.api.EmbedBuilder

@CommandMarker
class SlashLatest(private val versions: Versions) : ApplicationCommand() {
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
                builder.addJDA4Version()
                builder.addBlankField(true)
            }
            LibraryType.BOT_COMMANDS -> builder.addBCVersion()
            LibraryType.JDA5 -> builder.addJDA5Version()
            LibraryType.JDA4 -> builder.addJDA4Version()
        }

        event.replyEmbeds(builder.build())
            .addActionRow(getDeleteButton(event.user))
            .queue()
    }

    private fun EmbedBuilder.addJDA4Version() {
        addField("JDA 4", '`'.toString() + versions.latestJDA4Version.version + '`', true)
    }

    private fun EmbedBuilder.addJDA5Version() {
        addField("JDA 5", '`'.toString() + versions.latestJDA5Version.version + '`', true)
    }

    private fun EmbedBuilder.addBCVersion() {
        addField("BotCommands version", '`'.toString() + versions.latestBotCommandsVersion.version + '`', true)
        addField("JDA version for BC", '`'.toString() + versions.jdaVersionFromBotCommands.version + '`', true)
    }
}