package dev.freya02.doxxy.bot.commands.slash.versioning

import dev.freya02.doxxy.bot.commands.slash.DeleteButtonListener.Companion.messageDelete
import dev.freya02.doxxy.bot.utils.AppEmojis
import dev.freya02.doxxy.bot.utils.Utils.isBCGuild
import dev.freya02.doxxy.bot.versioning.LibraryType
import dev.freya02.doxxy.bot.versioning.ScriptType
import dev.freya02.doxxy.bot.versioning.github.*
import dev.freya02.doxxy.bot.versioning.github.PullRequest.Companion.toAutocompleteChoices
import dev.freya02.doxxy.bot.versioning.jitpack.JitpackBranchService
import dev.freya02.doxxy.bot.versioning.jitpack.JitpackPrService
import dev.freya02.doxxy.bot.versioning.jitpack.pullupdater.PullUpdater
import dev.freya02.doxxy.bot.versioning.supplier.BuildToolType
import dev.freya02.doxxy.bot.versioning.supplier.DependencySupplier
import dev.freya02.jda.emojis.unicode.Emojis
import dev.minn.jda.ktx.coroutines.await
import dev.minn.jda.ktx.interactions.components.*
import dev.minn.jda.ktx.messages.Embed
import dev.minn.jda.ktx.messages.MessageCreate
import dev.minn.jda.ktx.messages.reply_
import dev.minn.jda.ktx.messages.send
import io.github.freya022.botcommands.api.annotations.CommandMarker
import io.github.freya022.botcommands.api.commands.annotations.Command
import io.github.freya022.botcommands.api.commands.application.ApplicationCommand
import io.github.freya022.botcommands.api.commands.application.provider.GuildApplicationCommandManager
import io.github.freya022.botcommands.api.commands.application.provider.GuildApplicationCommandProvider
import io.github.freya022.botcommands.api.commands.application.slash.GuildSlashEvent
import io.github.freya022.botcommands.api.commands.application.slash.autocomplete.AutocompleteAlgorithms
import io.github.freya022.botcommands.api.commands.application.slash.autocomplete.annotations.AutocompleteHandler
import io.github.freya022.botcommands.api.commands.application.slash.builder.SlashCommandBuilder
import io.github.freya022.botcommands.api.components.Buttons
import io.github.freya022.botcommands.api.components.event.ButtonEvent
import io.github.freya022.botcommands.api.core.utils.edit
import io.github.freya022.botcommands.api.core.utils.runIgnoringResponse
import io.github.freya022.botcommands.api.core.utils.toEditData
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import net.dv8tion.jda.api.components.separator.Separator
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.interactions.Interaction
import net.dv8tion.jda.api.interactions.commands.Command.Choice
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import net.dv8tion.jda.api.requests.ErrorResponse
import net.dv8tion.jda.api.utils.TimeFormat
import net.dv8tion.jda.api.utils.messages.MessageCreateData
import kotlin.time.Duration.Companion.hours

@Command
class SlashJitpack(
    private val buttons: Buttons,
    private val jitpackPrService: JitpackPrService,
    private val jitpackBranchService: JitpackBranchService,
    private val client: GithubClient,
) : ApplicationCommand(), GuildApplicationCommandProvider {

    private data class AdditionalPullRequestDetails(
        val updatedPR: PullRequest,
        val commitComparisons: UpdatedCommitComparisons,
        val reverseCommitComparisons: CommitComparisons,
    )

    suspend fun onSlashJitpackPR(event: GuildSlashEvent, libraryType: LibraryType, buildToolType: BuildToolType, pullNumber: Int) {
        val pullRequest = jitpackPrService.getPullRequest(libraryType, pullNumber)
            ?: return event.reply_("Unknown Pull Request", ephemeral = true).queue()

        context(libraryType, buildToolType) {
            val message = createPrMessage(event, pullRequest, pullRequest.branch.toUpdatedBranch(), additionalDetails = null)

            event.reply(message).queue()

            val additionalDetails = coroutineScope {
                val updatedPR = async {
                    client.getPullRequest(libraryType.githubOwnerName, libraryType.githubRepoName, pullNumber)
                }

                val commitComparisons = async {
                    val base = pullRequest.base.label
                    val head = pullRequest.head.label

                    client.compareCommits(libraryType.githubOwnerName, libraryType.githubRepoName, base, head)
                        .toUpdatedCommitComparisons()
                }

                val reverseCommitComparisons = async {
                    val base = pullRequest.base.label
                    val head = pullRequest.head.label

                    client.compareCommits(libraryType.githubOwnerName, libraryType.githubRepoName, head, base)
                }

                AdditionalPullRequestDetails(updatedPR.await(), commitComparisons.await(), reverseCommitComparisons.await())
            }

            createPrMessage(event, additionalDetails.updatedPR, additionalDetails.updatedPR.branch.toUpdatedBranch(), additionalDetails)
                .toEditData()
                .edit(event.hook)
                .queue()
        }
    }

    context(libraryType: LibraryType, buildToolType: BuildToolType)
    private suspend fun createPrMessage(
        interaction: Interaction,
        pullRequest: PullRequest,
        targetBranch: UpdatedBranch,
        additionalDetails: AdditionalPullRequestDetails?,
    ): MessageCreateData {
        val dependencyStr: String = when (libraryType) {
            LibraryType.BOT_COMMANDS -> DependencySupplier.formatBCJitpack(
                ScriptType.DEPENDENCIES,
                buildToolType,
                jitpackBranchService.getUsedJDAVersionFromBranch(targetBranch),
                targetBranch.toJitpackArtifact()
            )
            LibraryType.JDA, LibraryType.JDA_KTX, LibraryType.LAVA_PLAYER -> DependencySupplier.formatJitpack(
                ScriptType.DEPENDENCIES,
                buildToolType,
                targetBranch.toJitpackArtifact()
            )
        }

        return MessageCreate {
            components += Container {
                +TextDisplay("### [${buildToolType.humanName} dependencies for ${libraryType.displayString}: ${pullRequest.title} (#${pullRequest.number})](${pullRequest.pullUrl})")
                +TextDisplay(when (buildToolType) {
                    BuildToolType.MAVEN -> "```xml\n$dependencyStr```"
                    BuildToolType.GRADLE, BuildToolType.GRADLE_KTS -> "```gradle\n$dependencyStr```"
                })
                +TextDisplay("-# *Remember to remove your existing JDA dependency before adding this*")

                if (additionalDetails != null) {
                    displayAdditionalDetails(interaction, additionalDetails)
                } else {
                    +TextDisplay("-# *Loading pull request details...*")
                }

                +Separator(isDivider = true, Separator.Spacing.SMALL)

                +ActionRow {
                    +link("https://jda.wiki/using-jda/using-new-features/", "How? (Wiki)", Emojis.FACE_WITH_MONOCLE)

                    +buttons.messageDelete(interaction.user)
                }
            }

            useComponentsV2 = true
        }
    }

    context(libraryType: LibraryType, _: BuildToolType)
    private suspend fun InlineContainer.displayAdditionalDetails(interaction: Interaction, additionalDetails: AdditionalPullRequestDetails) {
        val (_, commitComparisons, reverseCommitComparisons) = additionalDetails

        val behindText = when (commitComparisons.behindBy) {
            0 -> "behind"
            else -> "[behind](${reverseCommitComparisons.permalinkUrl})"
        }
        val branchStatus = TextDisplay("${AppEmojis.changesPush.formatted} ${commitComparisons.aheadBy} commits ahead, ${AppEmojis.changesUpdate.formatted} ${commitComparisons.behindBy} commits $behindText")

        if (jitpackPrService.canUsePullUpdate(libraryType)) {
            +Section(
                accessory = buttons.primary(label = "Update PR", emoji = AppEmojis.sync).ephemeral {
                    val callerId = interaction.user.idLong
                    timeout(1.hours)
                    bindTo {
                        onUpdatePrClick(it, callerId, additionalDetails)
                    }
                }
            ) {
                +branchStatus
            }
        } else {
            +branchStatus
        }

        displayMissingCommits(reverseCommitComparisons)
    }

    private fun InlineContainer.displayMissingCommits(reverseCommitComparisons: CommitComparisons) {
        fun CommitComparisons.Commit.asText(): String {
            return "[`${sha.asSha7}`](${htmlUrl}) ${commit.message} (${TimeFormat.RELATIVE.format(commit.committer.date)})"
        }

        // If the base branch is ahead (PR is missing updates)
        if (reverseCommitComparisons.aheadBy == 1) {
            +TextDisplay(reverseCommitComparisons.commits[0].asText())
        } else if (reverseCommitComparisons.aheadBy == 2) {
            +TextDisplay("""
                ${reverseCommitComparisons.commits[0].asText()}
                ${reverseCommitComparisons.commits[1].asText()}
            """.trimIndent())
        } else if (reverseCommitComparisons.aheadBy > 2) {
            +TextDisplay("""
                ${reverseCommitComparisons.commits.first().asText()}
                ...
                ${reverseCommitComparisons.commits.last().asText()}
            """.trimIndent())
        }
    }

    @AutocompleteHandler(name = PR_NUMBER_AUTOCOMPLETE_NAME, showUserInput = false)
    fun onPRNumberAutocomplete(
        event: CommandAutoCompleteInteractionEvent,
        libraryType: LibraryType
    ): Collection<Choice> {
        return jitpackPrService.getPullRequests(libraryType).toAutocompleteChoices(event)
    }

    context(libraryType: LibraryType, buildToolType: BuildToolType)
    private suspend fun onUpdatePrClick(event: ButtonEvent, callerId: Long, additionalDetails: AdditionalPullRequestDetails) {
        val (pullRequest) = additionalDetails

        event.deferEdit().queue()
        val waitMessage = when {
            PullUpdater.isRunning -> "Please wait while the pull request is being updated, this may be longer than usual"
            else -> "Please wait while the pull request is being updated"
        }.let { event.hook.send(it, ephemeral = true).await() }

        // Sometimes funny people delete the /jitpack message before the update has finished
        runIgnoringResponse(ErrorResponse.UNKNOWN_MESSAGE) {
            jitpackPrService.updatePr(libraryType, pullRequest.number, event.hook, waitMessage.idLong) { branch ->
                val message = createPrMessage(
                    event, pullRequest, branch,
                    additionalDetails.copy(
                        commitComparisons = additionalDetails.commitComparisons.copy(behindBy = 0),
                        reverseCommitComparisons = additionalDetails.reverseCommitComparisons.copy(aheadBy = 0)
                    )
                )
                if (event.user.idLong == callerId) {
                    event.hook.editOriginal(message.toEditData()).await()
                } else {
                    event.hook.sendMessage(message).setEphemeral(true).queue()
                }
            }
        }
    }

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

    override fun declareGuildApplicationCommands(manager: GuildApplicationCommandManager) {
        manager.slashCommand("jitpack", function = null) {
            description = "Shows you how to use jitpack for your bot"

            subcommand("branch", SlashJitpack::onSlashJitpackBranch) {
                description = "Shows you how to use a branch for your bot"

                addCommonJitpackOptions(manager)
                option("branchName") {
                    description = "The name of the Git branch to build from"
                    autocompleteByName(BRANCH_NAME_AUTOCOMPLETE_NAME)
                }
            }

            subcommand("pr", SlashJitpack::onSlashJitpackPR) {
                description = "Shows you how to use Pull Requests for your bot"

                addCommonJitpackOptions(manager)
                option("pullNumber") {
                    description = "The Pull Request number"
                    autocompleteByName(PR_NUMBER_AUTOCOMPLETE_NAME)
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

    @CommandMarker
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
            .addComponents(row(buttons.messageDelete(event.user)))
            .queue()
    }

    companion object {
        const val PR_NUMBER_AUTOCOMPLETE_NAME = "SlashJitpack: prNumber"
        private const val BRANCH_NAME_AUTOCOMPLETE_NAME = "SlashJitpack: branchName"
    }
}
