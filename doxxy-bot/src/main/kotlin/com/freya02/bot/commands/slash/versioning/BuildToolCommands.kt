package com.freya02.bot.commands.slash.versioning

import com.freya02.bot.commands.slash.DeleteButtonListener.Companion.messageDelete
import com.freya02.bot.commands.slash.SlashLogback
import com.freya02.bot.versioning.LibraryType
import com.freya02.bot.versioning.ScriptType
import com.freya02.bot.versioning.Versions
import com.freya02.bot.versioning.supplier.BuildToolType
import com.freya02.bot.versioning.supplier.DependencySupplier
import com.freya02.bot.versioning.supplier.GradleFlavor
import com.freya02.bot.versioning.supplier.UnsupportedDependencyException
import dev.minn.jda.ktx.messages.MessageCreate
import dev.minn.jda.ktx.messages.reply_
import io.github.freya022.botcommands.api.annotations.CommandMarker
import io.github.freya022.botcommands.api.commands.annotations.Command
import io.github.freya022.botcommands.api.commands.application.provider.GuildApplicationCommandManager
import io.github.freya022.botcommands.api.commands.application.provider.GuildApplicationCommandProvider
import io.github.freya022.botcommands.api.commands.application.slash.GuildSlashEvent
import io.github.freya022.botcommands.api.commands.application.slash.builder.TopLevelSlashCommandBuilder
import io.github.freya022.botcommands.api.components.Buttons
import io.github.freya022.botcommands.api.core.utils.asUnicodeEmoji
import io.github.oshai.kotlinlogging.KotlinLogging
import net.dv8tion.jda.api.interactions.components.ItemComponent
import net.dv8tion.jda.api.utils.FileUpload
import net.fellbaum.jemoji.Emojis
import kotlin.time.Duration.Companion.days

@Command
class BuildToolCommands(private val versions: Versions, private val buttons: Buttons, private val slashLogback: SlashLogback) : GuildApplicationCommandProvider {
    private val logger = KotlinLogging.logger { }

    @CommandMarker
    suspend fun onSlashBuildTool(
        event: GuildSlashEvent,
        scriptType: ScriptType,
        buildToolType: BuildToolType,
        libraryType: LibraryType = LibraryType.getDefaultLibrary(event.guild)
    ) {
        try {
            val script = when (libraryType) {
                LibraryType.BOT_COMMANDS -> DependencySupplier.formatBC(
                    scriptType,
                    buildToolType,
                    versions.jdaVersionFromBotCommands,
                    versions.latestBotCommandsVersion
                )
                LibraryType.JDA -> DependencySupplier.formatJDA(scriptType, buildToolType, versions.latestJDAVersion)
                LibraryType.JDA_KTX -> DependencySupplier.formatJDA(scriptType, buildToolType, versions.latestJDAKtxVersion)
                LibraryType.LAVA_PLAYER -> DependencySupplier.formatJDA(scriptType, buildToolType, versions.latestLavaPlayerVersion)
            }

            val messageData = MessageCreate {
                val components: MutableList<ItemComponent> = arrayListOf(buttons.messageDelete(event.user))

                when (scriptType) {
                    ScriptType.DEPENDENCIES -> {
                        embed {
                            title = "${buildToolType.humanName} dependencies for ${libraryType.displayString}"

                            description = "```${buildToolType.blockLang}\n$script```"
                        }
                    }
                    ScriptType.FULL -> {
                        if (buildToolType != BuildToolType.MAVEN) {
                            content = "```${buildToolType.blockLang}\n$script```"
                        } else {
                            files += FileUpload.fromData(script.encodeToByteArray(), "${buildToolType.fileName}.${buildToolType.fileExtension}")
                        }

                        components += buttons.secondary(
                            label = "Logback config",
                            emoji = Emojis.SCROLL.asUnicodeEmoji()
                        ).ephemeral {
                            timeout(1.days)
                            bindTo(slashLogback::onLogbackRequest)
                        }
                    }
                }

                actionRow(components)
            }

            event.reply(messageData).queue()
        } catch (e: UnsupportedDependencyException) {
            logger.debug { e.message }
            event.reply_(
                "The ${libraryType.displayString} ${buildToolType.humanName} ${scriptType.humanName.lowercase()} script isn't available yet.",
                ephemeral = true
            ).queue()
        }
    }

    @CommandMarker
    suspend fun onSlashGradle(
        event: GuildSlashEvent,
        scriptType: ScriptType,
        flavor: GradleFlavor,
        libraryType: LibraryType = LibraryType.getDefaultLibrary(event.guild)
    ) {
        val buildToolType = when (flavor) {
            GradleFlavor.KOTLIN -> BuildToolType.GRADLE_KTS
            GradleFlavor.GROOVY -> BuildToolType.GRADLE
        }
        return onSlashBuildTool(event, scriptType, buildToolType, libraryType)
    }

    override fun declareGuildApplicationCommands(manager: GuildApplicationCommandManager) {
        manager.slashCommand("maven", function = BuildToolCommands::onSlashBuildTool) {
            description = "Shows the Maven dependencies for a library (default: ${LibraryType.getDefaultLibrary(manager.guild).displayString})"

            generatedOption("buildToolType") { BuildToolType.MAVEN }

            addCommonOptions()
        }

        manager.slashCommand("gradle", function = BuildToolCommands::onSlashGradle) {
            description = "Shows the Gradle dependencies for a library (default: ${LibraryType.getDefaultLibrary(manager.guild).displayString})"

            option("flavor") {
                description = "The language the gradle script should be in"
                usePredefinedChoices = true
            }

            addCommonOptions()
        }
    }

    private fun TopLevelSlashCommandBuilder.addCommonOptions() {
        option("scriptType") {
            description = "Whether to show the full build script or only the dependencies"
            usePredefinedChoices = true
        }

        option(declaredName = "libraryType", optionName = "library") {
            description = "Type of library"
            usePredefinedChoices = true
        }
    }
}
