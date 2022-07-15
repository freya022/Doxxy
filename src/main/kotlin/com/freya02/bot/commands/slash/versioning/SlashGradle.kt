package com.freya02.bot.commands.slash.versioning

import com.freya02.bot.commands.slash.DeleteButtonListener.Companion.getDeleteButton
import com.freya02.bot.utils.Utils.isBCGuild
import com.freya02.bot.versioning.LibraryType
import com.freya02.bot.versioning.Versions
import com.freya02.bot.versioning.supplier.BuildToolType
import com.freya02.bot.versioning.supplier.DependencySupplier
import com.freya02.botcommands.api.annotations.CommandMarker
import com.freya02.botcommands.api.application.ApplicationCommand
import com.freya02.botcommands.api.application.annotations.AppOption
import com.freya02.botcommands.api.application.slash.GuildSlashEvent
import com.freya02.botcommands.api.application.slash.annotations.JDASlashCommand
import dev.minn.jda.ktx.messages.Embed

@CommandMarker
class SlashGradle(private val versions: Versions) : ApplicationCommand() {
    @JDASlashCommand(name = "gradle", description = "Shows the Gradle dependencies for a library")
    fun onSlashGradle(
        event: GuildSlashEvent,
        @AppOption(description = "Type of library") libraryType: LibraryType?
    ) {
        val libraryType = libraryType ?: run {
            if (event.guild.isBCGuild()) LibraryType.BOT_COMMANDS else LibraryType.JDA5
        }

        val script = when (libraryType) {
            LibraryType.BOT_COMMANDS -> DependencySupplier.formatBC(
                BuildToolType.GRADLE,
                versions.jdaVersionFromBotCommands,
                versions.latestBotCommandsVersion
            )
            LibraryType.JDA5 -> DependencySupplier.formatJDA5(BuildToolType.GRADLE, versions.latestJDA5Version)
            LibraryType.JDA4 -> DependencySupplier.formatJDA4(BuildToolType.GRADLE, versions.latestJDA4Version)
        }

        val embed = Embed {
            title = "Gradle dependencies for ${libraryType.displayString}"

            description = "```gradle\n$script```"
        }

        event.replyEmbeds(embed)
            .addActionRow(getDeleteButton(event.user))
            .queue()
    }
}