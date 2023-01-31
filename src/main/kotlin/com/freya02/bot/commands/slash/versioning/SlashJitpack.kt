package com.freya02.bot.commands.slash.versioning

import com.freya02.bot.commands.slash.DeleteButtonListener.Companion.messageDeleteButton
import com.freya02.bot.utils.Utils.isBCGuild
import com.freya02.bot.versioning.LibraryType
import com.freya02.bot.versioning.ScriptType
import com.freya02.bot.versioning.github.GithubBranch
import com.freya02.bot.versioning.github.PullRequest
import com.freya02.bot.versioning.jitpack.JitpackBranchService
import com.freya02.bot.versioning.jitpack.JitpackPrService
import com.freya02.bot.versioning.supplier.BuildToolType
import com.freya02.bot.versioning.supplier.DependencySupplier
import com.freya02.botcommands.api.annotations.CommandMarker
import com.freya02.botcommands.api.commands.application.ApplicationCommand
import com.freya02.botcommands.api.commands.application.CommandScope
import com.freya02.botcommands.api.commands.application.GuildApplicationCommandManager
import com.freya02.botcommands.api.commands.application.annotations.AppDeclaration
import com.freya02.botcommands.api.commands.application.annotations.AppOption
import com.freya02.botcommands.api.commands.application.slash.GuildSlashEvent
import com.freya02.botcommands.api.commands.application.slash.autocomplete.AutocompleteAlgorithms
import com.freya02.botcommands.api.commands.application.slash.autocomplete.FuzzyResult
import com.freya02.botcommands.api.commands.application.slash.autocomplete.ToStringFunction
import com.freya02.botcommands.api.commands.application.slash.autocomplete.annotations.AutocompleteHandler
import com.freya02.botcommands.api.commands.application.slash.autocomplete.annotations.CacheAutocomplete
import com.freya02.botcommands.api.commands.application.slash.autocomplete.annotations.CompositeKey
import com.freya02.botcommands.api.commands.application.slash.builder.SlashCommandBuilder
import com.freya02.botcommands.api.components.Components
import com.freya02.botcommands.api.utils.EmojiUtils
import dev.minn.jda.ktx.interactions.components.link
import dev.minn.jda.ktx.messages.Embed
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.interactions.commands.Command.Choice
import net.dv8tion.jda.api.interactions.commands.build.OptionData

@CommandMarker
class SlashJitpack(
    private val components: Components,
    private val jitpackPrService: JitpackPrService,
    private val jitpackBranchService: JitpackBranchService
) : ApplicationCommand() {
    @CommandMarker
    fun onSlashJitpackPR(event: GuildSlashEvent, libraryType: LibraryType, buildToolType: BuildToolType, pullNumber: Int) {
        val pullRequest = jitpackPrService.getPullRequest(libraryType, pullNumber) ?: run {
            event.reply("Unknown Pull Request").setEphemeral(true).queue()
            return
        }

        val dependencyStr: String = when (libraryType) {
            LibraryType.BOT_COMMANDS -> DependencySupplier.formatBCJitpack(
                ScriptType.DEPENDENCIES,
                buildToolType,
                jitpackBranchService.getUsedJDAVersionFromBranch(pullRequest.branch),
                pullRequest.toJitpackArtifact()
            )
            LibraryType.JDA, LibraryType.JDA_KTX, LibraryType.LAVA_PLAYER -> DependencySupplier.formatJitpack(
                ScriptType.DEPENDENCIES,
                buildToolType,
                pullRequest.toJitpackArtifact()
            )
        }

        val embed = Embed {
            title =
                "${buildToolType.humanName} dependencies for ${libraryType.displayString} @ PR #${pullRequest.number}"
            url = pullRequest.pullUrl

            field("PR Link", pullRequest.pullUrl, false)

            description = when (buildToolType) {
                BuildToolType.MAVEN -> "```xml\n$dependencyStr```"
                BuildToolType.GRADLE, BuildToolType.GRADLE_KTS -> "```gradle\n$dependencyStr```"
            }
        }

        event.replyEmbeds(embed)
            .addActionRow(
                components.messageDeleteButton(event.user),
                link(
                    "https://jda.wiki/using-jda/using-new-features/",
                    "How ? (Wiki)",
                    EmojiUtils.resolveJDAEmoji("face_with_monocle")
                )
            )
            .queue()
    }

    @CacheAutocomplete
    @AutocompleteHandler(name = PR_NUMBER_AUTOCOMPLETE_NAME, showUserInput = false)
    fun onPRNumberAutocomplete(
        event: CommandAutoCompleteInteractionEvent,
        @CompositeKey @AppOption libraryType: LibraryType
    ): Collection<Choice> {
        val pullRequests = jitpackPrService.getPullRequests(libraryType)

        return pullRequests
            .fuzzyMatching(
                { referent: PullRequest -> referent.title + referent.branch.authorName }, //Don't autocomplete based on the branch number
                event.focusedOption.value
            )
            .map { r ->
                Choice(
                    r.item.asHumanDescription,
                    r.item.number.toLong()
                )
            }
    }

    @CacheAutocomplete
    @AutocompleteHandler(name = BRANCH_NAME_AUTOCOMPLETE_NAME, showUserInput = false)
    fun onBranchNameAutocomplete(
        event: CommandAutoCompleteInteractionEvent,
        @CompositeKey @AppOption libraryType: LibraryType
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

    @AppDeclaration
    fun declare(manager: GuildApplicationCommandManager) {
        manager.slashCommand("jitpack", CommandScope.GUILD) {
            description = "Shows you how to use jitpack for your bot"

            subcommandGroup("branch") {
                description = "Shows you how to use a branch for your bot"

                BuildToolType.values().forEach { toolType ->
                    subcommand(toolType.cmdName) {
                        description = "Shows you how to use a branch for your bot"

                        addCommonJitpackOptions(manager, toolType)
                        option("branchName") {
                            autocompleteReference(BRANCH_NAME_AUTOCOMPLETE_NAME)
                        }

                        function = SlashJitpack::onSlashJitpackBranch
                    }
                }
            }

            subcommandGroup("pr") {
                description = "Shows you how to use Pull Requests for your bot"

                BuildToolType.values().forEach { toolType ->
                    subcommand(toolType.cmdName) {
                        description = "Shows you how to use Pull Requests for your bot"

                        addCommonJitpackOptions(manager, toolType)
                        option("pullNumber") {
                            description = "The number of the issue"

                            autocompleteReference(PR_NUMBER_AUTOCOMPLETE_NAME)
                        }

                        function = SlashJitpack::onSlashJitpackPR
                    }
                }
            }
        }
    }

    private fun SlashCommandBuilder.addCommonJitpackOptions(manager: GuildApplicationCommandManager, toolType: BuildToolType) {
        option("libraryType") {
            description = "Type of library"

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

        generatedOption("buildToolType") { toolType }
    }

    @CommandMarker
    fun onSlashJitpackBranch(event: GuildSlashEvent, libraryType: LibraryType, buildToolType: BuildToolType, branchName: String?) {
        val branch = jitpackBranchService.getBranch(libraryType, branchName) ?: run {
            event.reply("Unknown branch '$branchName'").setEphemeral(true).queue()
            return
        }

        onSlashJitpackBranchImpl(event, libraryType, buildToolType, branch)
    }

    private fun onSlashJitpackBranchImpl(
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
                jitpackBranchService.getUsedJDAVersionFromBranch(branch),
                branch.toJitpackArtifact()
            )
        }

        val embed = Embed {
            title = "${buildToolType.humanName} dependencies for ${libraryType.displayString} @ branch '$branchName'"
            url = branch.toURL()

            field("Branch Link", branch.toURL(), false)

            description = when (buildToolType) {
                BuildToolType.MAVEN -> "```xml\n$dependencyStr```"
                BuildToolType.GRADLE, BuildToolType.GRADLE_KTS -> "```gradle\n$dependencyStr```"
            }
        }

        event.replyEmbeds(embed)
            .addActionRow(components.messageDeleteButton(event.user))
            .queue()
    }

    companion object {
        const val PR_NUMBER_AUTOCOMPLETE_NAME = "SlashJitpack: prNumber"
        private const val BRANCH_NAME_AUTOCOMPLETE_NAME = "SlashJitpack: branchName"

        private fun Collection<PullRequest>.fuzzyMatching(
            toStringFunction: ToStringFunction<PullRequest>,
            query: String
        ): Collection<FuzzyResult<PullRequest>> {
            if (query.firstOrNull()?.isDigit() == true) {
                return filter { it.number.toString().startsWith(query) }
                    .take(OptionData.MAX_CHOICES)
                    .map { FuzzyResult(it, "", 0.0) }
            }

            return sortedWith(Comparator.comparingInt { obj: PullRequest -> obj.number }.reversed()).let {
                when {
                    query.isBlank() -> take(OptionData.MAX_CHOICES).map { FuzzyResult(it, "", 0.0) }
                    else -> AutocompleteAlgorithms.fuzzyMatching(this, toStringFunction, query).take(OptionData.MAX_CHOICES)
                }
            }
        }
    }
}
