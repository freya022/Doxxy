package dev.freya02.doxxy.bot.commands.slash.versioning

import dev.freya02.doxxy.bot.utils.Utils.isBCGuild
import dev.freya02.doxxy.bot.versioning.LibraryType
import io.github.freya022.botcommands.api.commands.annotations.Command
import io.github.freya022.botcommands.api.commands.application.provider.GuildApplicationCommandManager
import io.github.freya022.botcommands.api.commands.application.provider.GuildApplicationCommandProvider
import io.github.freya022.botcommands.api.commands.application.slash.builder.SlashCommandBuilder
import net.dv8tion.jda.api.interactions.commands.Command.Choice

@Command
class SlashJitpackCommandProvider : GuildApplicationCommandProvider {

    override fun declareGuildApplicationCommands(manager: GuildApplicationCommandManager) {
        manager.slashCommand("jitpack", function = null) {
            description = "Shows you how to use jitpack for your bot"

            subcommand("branch", SlashJitpackBranchController::onSlashJitpackBranch) {
                description = "Shows you how to use a branch for your bot"

                addCommonJitpackOptions(manager)
                option("branchName") {
                    description = "The name of the Git branch to build from"
                    autocompleteByName(SlashJitpackBranchController.BRANCH_NAME_AUTOCOMPLETE_NAME)
                }
            }

            subcommand("pr", SlashJitpackPrController::onSlashJitpackPR) {
                description = "Shows you how to use Pull Requests for your bot"

                addCommonJitpackOptions(manager)
                option("pullNumber") {
                    description = "The Pull Request number"
                    autocompleteByName(SlashJitpackPrController.PR_NUMBER_AUTOCOMPLETE_NAME)
                }
            }
        }
    }

    private fun SlashCommandBuilder.addCommonJitpackOptions(manager: GuildApplicationCommandManager) {
        option(declaredName = "libraryType", optionName = "library") {
            description = "The target library"

            //Override choices are already set in LibraryTypeResolver as this command could not cover all entries in the future
            choices = when {
                manager.guild.isBCGuild() -> listOf(
                    Choice("BotCommands", LibraryType.BOT_COMMANDS.name),
                    Choice("JDA", LibraryType.JDA.name),
                    Choice("JDA-KTX", LibraryType.JDA_KTX.name)
                )
                else -> listOf(
                    Choice("JDA", LibraryType.JDA.name),
                    Choice("JDA-KTX", LibraryType.JDA_KTX.name),
                    Choice("LavaPlayer", LibraryType.LAVA_PLAYER.name)
                )
            }
        }

        option(declaredName = "buildToolType", optionName = "build_tool") {
            description = "The build tool to generate the script for"
            usePredefinedChoices = true
        }
    }
}