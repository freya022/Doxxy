package com.freya02.bot.commands.slash.versioning

import com.freya02.bot.commands.slash.DeleteButtonListener.Companion.messageDeleteButton
import com.freya02.bot.utils.Utils.isBCGuild
import com.freya02.bot.versioning.LibraryType
import com.freya02.bot.versioning.Versions
import com.freya02.bot.versioning.supplier.BuildToolType
import com.freya02.bot.versioning.supplier.DependencySupplier
import com.freya02.botcommands.api.annotations.CommandMarker
import com.freya02.botcommands.api.commands.application.ApplicationCommand
import com.freya02.botcommands.api.commands.application.annotations.AppOption
import com.freya02.botcommands.api.commands.application.slash.GuildSlashEvent
import com.freya02.botcommands.api.commands.application.slash.annotations.JDASlashCommand
import com.freya02.botcommands.api.components.Components
import dev.minn.jda.ktx.messages.Embed

@CommandMarker
class SlashMaven(private val versions: Versions, private val components: Components) : ApplicationCommand() {
    @JDASlashCommand(name = "maven", description = "Shows the Maven dependencies for a library")
    fun onSlashMaven(
        event: GuildSlashEvent,
        @AppOption(description = "Type of library") libraryType: LibraryType?
    ) {
        val libraryType = libraryType ?: run {
            if (event.guild.isBCGuild()) LibraryType.BOT_COMMANDS else LibraryType.JDA5
        }

        val xml = when (libraryType) {
            LibraryType.BOT_COMMANDS -> DependencySupplier.formatBC(
                BuildToolType.MAVEN,
                versions.jdaVersionFromBotCommands,
                versions.latestBotCommandsVersion
            )
            LibraryType.JDA5 -> DependencySupplier.formatJDA5(BuildToolType.MAVEN, versions.latestJDA5Version)
            LibraryType.JDA4 -> DependencySupplier.formatJDA4(BuildToolType.MAVEN, versions.latestJDA4Version)
        }

        val embed = Embed {
            title = "Maven dependencies for ${libraryType.displayString}"

            description = "```xml\n$xml```"
        }

        event.replyEmbeds(embed)
            .addActionRow(components.messageDeleteButton(event.user))
            .queue()
    }
}