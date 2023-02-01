package com.freya02.bot.commands.slash.versioning

import com.freya02.bot.commands.slash.DeleteButtonListener.Companion.messageDeleteButton
import com.freya02.bot.versioning.LibraryType
import com.freya02.bot.versioning.ScriptType
import com.freya02.bot.versioning.Versions
import com.freya02.bot.versioning.supplier.BuildToolType
import com.freya02.bot.versioning.supplier.DependencySupplier
import com.freya02.bot.versioning.supplier.UnsupportedDependencyException
import com.freya02.botcommands.api.annotations.CommandMarker
import com.freya02.botcommands.api.commands.application.CommandScope
import com.freya02.botcommands.api.commands.application.GuildApplicationCommandManager
import com.freya02.botcommands.api.commands.application.annotations.AppDeclaration
import com.freya02.botcommands.api.commands.application.slash.GuildSlashEvent
import com.freya02.botcommands.api.components.Components
import dev.minn.jda.ktx.messages.MessageCreate
import dev.minn.jda.ktx.messages.reply_
import mu.KotlinLogging
import net.dv8tion.jda.api.utils.FileUpload

@CommandMarker
class BuildToolCommands(private val versions: Versions, private val components: Components) {
    private val logger = KotlinLogging.logger { }

    @CommandMarker
    fun onSlashBuildTool(
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
                LibraryType.JDA_KTX -> DependencySupplier.formatJitpack(scriptType, buildToolType, versions.latestJDAKtxVersion)
                LibraryType.LAVA_PLAYER -> DependencySupplier.formatJitpack(scriptType, buildToolType, versions.latestLavaPlayerVersion)
            }

            val messageData = MessageCreate {
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
                    }
                }

                actionRow(this@BuildToolCommands.components.messageDeleteButton(event.user))
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

    @AppDeclaration
    fun declare(manager: GuildApplicationCommandManager) {
        for (buildToolType in BuildToolType.values()) {
            manager.slashCommand(buildToolType.cmdName, CommandScope.GUILD) {
                description = "Shows the ${buildToolType.humanName} dependencies for a library (default: ${LibraryType.getDefaultLibrary(manager.guild).displayString})"

                option("scriptType") {
                    description = "Whether to show the full build script or only the dependencies"
                    usePredefinedChoices = true
                }

                generatedOption("buildToolType") {
                    buildToolType
                }

                option("libraryType") {
                    description = "Type of library"
                    usePredefinedChoices = true
                }

                function = ::onSlashBuildTool
            }
        }
    }
}
