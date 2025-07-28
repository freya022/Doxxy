package dev.freya02.doxxy.bot.commands.slash.versioning

import dev.freya02.botcommands.jda.ktx.components.row
import dev.freya02.doxxy.bot.commands.slash.DeleteButtonListener.Companion.messageDelete
import dev.freya02.doxxy.bot.utils.Utils.isBCGuild
import dev.freya02.doxxy.bot.utils.Utils.isJDAGuild
import dev.freya02.doxxy.bot.versioning.LibraryType
import dev.freya02.doxxy.bot.versioning.Versions
import io.github.freya022.botcommands.api.commands.annotations.Command
import io.github.freya022.botcommands.api.commands.application.ApplicationCommand
import io.github.freya022.botcommands.api.commands.application.CommandScope
import io.github.freya022.botcommands.api.commands.application.slash.GuildSlashEvent
import io.github.freya022.botcommands.api.commands.application.slash.annotations.JDASlashCommand
import io.github.freya022.botcommands.api.commands.application.slash.annotations.SlashOption
import io.github.freya022.botcommands.api.commands.application.slash.annotations.TopLevelSlashCommandData
import io.github.freya022.botcommands.api.components.Buttons
import net.dv8tion.jda.api.EmbedBuilder

@Command
class SlashLatest(private val versions: Versions, private val buttons: Buttons) : ApplicationCommand() {
    @JDASlashCommand(name = "latest", description = "Shows the latest version of the library")
    @TopLevelSlashCommandData(scope = CommandScope.GUILD)
    suspend fun onSlashLatest(
        event: GuildSlashEvent,
        @SlashOption(name = "library", description = "The target library", usePredefinedChoices = true) libraryType: LibraryType?
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
            .addComponents(row(buttons.messageDelete(event.user)))
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
