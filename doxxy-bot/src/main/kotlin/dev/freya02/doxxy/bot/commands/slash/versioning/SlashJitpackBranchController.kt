package dev.freya02.doxxy.bot.commands.slash.versioning

import dev.freya02.doxxy.bot.commands.slash.DeleteButtonListener.Companion.messageDelete
import dev.freya02.doxxy.bot.versioning.LibraryType
import dev.freya02.doxxy.bot.versioning.ScriptType
import dev.freya02.doxxy.bot.versioning.github.GithubBranch
import dev.freya02.doxxy.bot.versioning.github.toUpdatedBranch
import dev.freya02.doxxy.bot.versioning.jitpack.JitpackBranchService
import dev.freya02.doxxy.bot.versioning.supplier.BuildToolType
import dev.freya02.doxxy.bot.versioning.supplier.DependencySupplier
import dev.minn.jda.ktx.interactions.components.ActionRow
import dev.minn.jda.ktx.interactions.components.Container
import dev.minn.jda.ktx.interactions.components.TextDisplay
import io.github.freya022.botcommands.api.commands.application.slash.GuildSlashEvent
import io.github.freya022.botcommands.api.commands.application.slash.autocomplete.AutocompleteAlgorithms
import io.github.freya022.botcommands.api.commands.application.slash.autocomplete.annotations.AutocompleteHandler
import io.github.freya022.botcommands.api.components.Buttons
import io.github.freya022.botcommands.api.core.annotations.Handler
import io.github.freya022.botcommands.api.core.utils.reply
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.interactions.commands.Command.Choice
import net.dv8tion.jda.api.interactions.commands.build.OptionData

@Handler
class SlashJitpackBranchController(
    private val buttons: Buttons,
    private val jitpackBranchService: JitpackBranchService,
) {

    @AutocompleteHandler(name = BRANCH_NAME_AUTOCOMPLETE_NAME, showUserInput = false)
    suspend fun onBranchNameAutocomplete(
        event: CommandAutoCompleteInteractionEvent,
        libraryType: LibraryType
    ): Collection<Choice> {
        val branchMap = jitpackBranchService.getBranchMap(libraryType)
        val defaultBranchName = branchMap.defaultBranch.branchName

        return AutocompleteAlgorithms.fuzzyMatching(branchMap.branches.keys, { it }, event.focusedOption.value)
            .take(OptionData.MAX_CHOICES)
            .map { it.string }
            .let { branchNames ->
                return@let when {
                    event.focusedOption.value.isNotBlank() -> branchNames //Don't hoist if user is searching
                    else -> branchNames.toMutableList() // Hoist default branch to the top
                        .apply {
                            remove(defaultBranchName)
                            add(0, defaultBranchName)
                        }
                }
            }
            .map { branchName ->
                val choiceLabel = when (branchName) {
                    defaultBranchName -> "$branchName (default)"
                    else -> branchName
                }

                Choice(choiceLabel, branchName)
            }
    }

    suspend fun onSlashJitpackBranch(event: GuildSlashEvent, libraryType: LibraryType, buildToolType: BuildToolType, branchName: String?) {
        val branch = jitpackBranchService.getBranch(libraryType, branchName) ?: run {
            event.reply("Unknown branch '$branchName'").setEphemeral(true).queue()
            return
        }

        onSlashJitpackBranchImpl(event, libraryType, buildToolType, branch)
    }

    private suspend fun onSlashJitpackBranchImpl(
        event: GuildSlashEvent,
        libraryType: LibraryType,
        buildToolType: BuildToolType,
        branch: GithubBranch
    ) {
        val branchName = branch.branchName

        val dependencyStr = when (libraryType) {
            LibraryType.JDA, LibraryType.JDA_KTX, LibraryType.LAVA_PLAYER -> DependencySupplier.formatJitpack(
                ScriptType.DEPENDENCIES,
                buildToolType,
                branch.toJitpackArtifact()
            )
            LibraryType.BOT_COMMANDS -> DependencySupplier.formatBCJitpack(
                ScriptType.DEPENDENCIES,
                buildToolType,
                jitpackBranchService.getUsedJDAVersionFromBranch(branch.toUpdatedBranch()),
                branch.toJitpackArtifact()
            )
        }

        event.reply {
            components += Container {
                +TextDisplay("### [${buildToolType.humanName} dependencies for ${libraryType.displayString} @ branch '$branchName'](${branch.toURL()})")
                +TextDisplay(when (buildToolType) {
                    BuildToolType.MAVEN -> "```xml\n$dependencyStr```"
                    BuildToolType.GRADLE, BuildToolType.GRADLE_KTS -> "```gradle\n$dependencyStr```"
                })
                +TextDisplay("-# *Remember to remove your existing JDA dependency before adding this*")
            }

            components += ActionRow {
                +buttons.messageDelete(event.user)
            }

            useComponentsV2 = true
        }.queue()
    }

    companion object {
        const val BRANCH_NAME_AUTOCOMPLETE_NAME = "SlashJitpack: branchName"
    }
}
