package dev.freya02.doxxy.bot.commands.slash.versioning

import dev.freya02.botcommands.jda.ktx.messages.reply_
import dev.freya02.doxxy.bot.commands.slash.DeleteButtonListener.Companion.messageDelete
import dev.freya02.doxxy.bot.versioning.ArtifactInfo
import dev.freya02.doxxy.bot.versioning.LibraryType
import dev.freya02.doxxy.bot.versioning.ScriptType
import dev.freya02.doxxy.bot.versioning.jitpack.JitpackBranchService
import dev.freya02.doxxy.bot.versioning.jitpack.pullupdater.UpdatedBranch
import dev.freya02.doxxy.bot.versioning.supplier.BuildToolType
import dev.freya02.doxxy.bot.versioning.supplier.DependencySupplier
import dev.freya02.doxxy.github.client.data.Branches
import io.github.freya022.botcommands.api.commands.application.slash.GuildSlashEvent
import io.github.freya022.botcommands.api.commands.application.slash.autocomplete.AutocompleteAlgorithms
import io.github.freya022.botcommands.api.commands.application.slash.autocomplete.annotations.AutocompleteHandler
import io.github.freya022.botcommands.api.components.Buttons
import io.github.freya022.botcommands.api.core.annotations.Handler
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.interactions.commands.Command.Choice
import net.dv8tion.jda.api.interactions.commands.build.OptionData

@Handler
class SlashJitpackBranch(
    private val buttons: Buttons,
    private val jitpackBranchService: JitpackBranchService,
) {

    @AutocompleteHandler(name = BRANCH_NAME_AUTOCOMPLETE_NAME, showUserInput = false)
    suspend fun onBranchNameAutocomplete(
        event: CommandAutoCompleteInteractionEvent,
        libraryType: LibraryType
    ): Collection<Choice> {
        val branchMap = jitpackBranchService.getBranchMap(libraryType)
        val defaultBranchName = branchMap.defaultBranch.name

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
        val branch = jitpackBranchService.getBranch(libraryType, branchName)
            ?: return event.reply_("Unknown branch '$branchName'", ephemeral = true).queue()

        onSlashJitpackBranchImpl(event, libraryType, buildToolType, branch)
    }

    private suspend fun onSlashJitpackBranchImpl(
        event: GuildSlashEvent,
        libraryType: LibraryType,
        buildToolType: BuildToolType,
        branch: Branches.Branch,
    ) {
        val branchName = branch.name

        val dependencyStr = when (libraryType) {
            LibraryType.JDA, LibraryType.JDA_KTX, LibraryType.LAVA_PLAYER -> DependencySupplier.formatJitpack(
                ScriptType.DEPENDENCIES,
                buildToolType,
                branch.toJitpackArtifact(libraryType)
            )
            LibraryType.BOT_COMMANDS -> DependencySupplier.formatBCJitpack(
                ScriptType.DEPENDENCIES,
                buildToolType,
                branch.toJitpackArtifact(libraryType)
            )
        }

        event.reply_(useComponentsV2 = true) {
            container {
                text("### [${buildToolType.humanName} dependencies for ${libraryType.displayString} @ branch '$branchName'](${branch.toUrl(libraryType)})")
                text(when (buildToolType) {
                    BuildToolType.MAVEN -> "```xml\n$dependencyStr```"
                    BuildToolType.GRADLE, BuildToolType.GRADLE_KTS -> "```gradle\n$dependencyStr```"
                })
                text("-# *Remember to remove your existing dependency before adding this*")
            }

            actionRow {
                components += buttons.messageDelete(event.user)
            }
        }.queue()
    }

    private fun Branches.Branch.toUrl(libraryType: LibraryType): String =
        "https://github.com/${libraryType.githubOwnerName}/${libraryType.githubRepoName}/tree/$name"

    private fun Branches.Branch.toJitpackArtifact(libraryType: LibraryType): ArtifactInfo = ArtifactInfo(
        "io.github.${libraryType.githubOwnerName}",
        libraryType.githubRepoName,
        commit.sha.asSha10,
    )

    private fun Branches.Branch.toUpdatedBranch(libraryType: LibraryType): UpdatedBranch =
        UpdatedBranch(libraryType.githubOwnerName, libraryType.githubRepoName, name, commit.sha)

    companion object {
        const val BRANCH_NAME_AUTOCOMPLETE_NAME = "SlashJitpack: branchName"
    }
}
