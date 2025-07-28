package dev.freya02.doxxy.bot.commands.slash.versioning

import dev.freya02.botcommands.jda.ktx.components.*
import dev.freya02.botcommands.jda.ktx.messages.*
import dev.freya02.botcommands.jda.ktx.requests.queueIgnoring
import dev.freya02.doxxy.bot.commands.slash.DeleteButtonListener.Companion.messageDelete
import dev.freya02.doxxy.bot.utils.AppEmojis
import dev.freya02.doxxy.bot.utils.github.toAutocompleteChoices
import dev.freya02.doxxy.bot.versioning.ArtifactInfo
import dev.freya02.doxxy.bot.versioning.LibraryType
import dev.freya02.doxxy.bot.versioning.ScriptType
import dev.freya02.doxxy.bot.versioning.jitpack.JitpackBranchService
import dev.freya02.doxxy.bot.versioning.jitpack.JitpackPrService
import dev.freya02.doxxy.bot.versioning.jitpack.JitpackPrService.AdditionalPullRequestDetails
import dev.freya02.doxxy.bot.versioning.jitpack.pullupdater.UpdatedBranch
import dev.freya02.doxxy.bot.versioning.supplier.BuildToolType
import dev.freya02.doxxy.bot.versioning.supplier.DependencySupplier
import dev.freya02.doxxy.github.client.data.Branch
import dev.freya02.doxxy.github.client.data.CommitComparisons
import dev.freya02.doxxy.github.client.data.PullRequest
import dev.freya02.jda.emojis.unicode.Emojis
import io.github.freya022.botcommands.api.commands.application.slash.GuildSlashEvent
import io.github.freya022.botcommands.api.commands.application.slash.autocomplete.annotations.AutocompleteHandler
import io.github.freya022.botcommands.api.components.Buttons
import io.github.freya022.botcommands.api.components.event.ButtonEvent
import io.github.freya022.botcommands.api.core.annotations.Handler
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.interactions.Interaction
import net.dv8tion.jda.api.interactions.commands.Command.Choice
import net.dv8tion.jda.api.requests.ErrorResponse
import net.dv8tion.jda.api.utils.TimeFormat
import net.dv8tion.jda.api.utils.messages.MessageCreateData
import kotlin.time.Duration.Companion.hours

@Handler
class SlashJitpackPr(
    private val buttons: Buttons,
    private val jitpackPrService: JitpackPrService,
    private val jitpackBranchService: JitpackBranchService,
) {

    suspend fun onSlashJitpackPR(event: GuildSlashEvent, libraryType: LibraryType, buildToolType: BuildToolType, pullNumber: Int) {
        val pullRequest = jitpackPrService.getPullRequest(libraryType, pullNumber)
            ?: return event.reply_("Unknown Pull Request", ephemeral = true).queue()

        context(libraryType, buildToolType) {
            val message = createPrMessage(event, pullRequest, pullRequest.branch.toUpdatedBranch(), additionalDetails = null)

            event.reply(message).queue()

            jitpackPrService.fetchAdditionalPRDetails(libraryType, pullRequest) { additionalDetails ->
                createPrMessage(event, additionalDetails.updatedPR, additionalDetails.updatedPR.branch.toUpdatedBranch(), additionalDetails)
                    .toEditData()
                    .edit(event.hook)
                    .queue()
            }
        }
    }

    context(libraryType: LibraryType, buildToolType: BuildToolType)
    private suspend fun createPrMessage(
        interaction: Interaction,
        pullRequest: PullRequest,
        targetBranch: UpdatedBranch,
        additionalDetails: AdditionalPullRequestDetails?,
        updating: Boolean = false,
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

        return MessageCreate(useComponentsV2 = true) {
            components += Container {
                +TextDisplay("### [${buildToolType.humanName} dependencies for ${libraryType.displayString}: ${pullRequest.title} (#${pullRequest.number})](${pullRequest.pullUrl})")
                +TextDisplay(when (buildToolType) {
                    BuildToolType.MAVEN -> "```xml\n$dependencyStr```"
                    BuildToolType.GRADLE, BuildToolType.GRADLE_KTS -> "```gradle\n$dependencyStr```"
                })
                +TextDisplay("-# *Remember to remove your existing dependency before adding this*")

                if (additionalDetails != null) {
                    displayAdditionalDetails(targetBranch, additionalDetails, updating)
                } else {
                    +TextDisplay("-# *Loading pull request details...*")
                }
            }

            components += ActionRow {
                link("https://jda.wiki/using-jda/using-new-features/", "How? (Wiki)", Emojis.FACE_WITH_MONOCLE)

                +buttons.messageDelete(interaction.user)
            }
        }
    }

    context(libraryType: LibraryType, _: BuildToolType)
    private suspend fun InlineContainer.displayAdditionalDetails(
        targetBranch: UpdatedBranch,
        additionalDetails: AdditionalPullRequestDetails,
        updating: Boolean,
    ) {
        val (pullRequest, commitComparisons, reverseCommitComparisons) = additionalDetails

        val behindText = when (commitComparisons.behindBy) {
            0 -> "behind"
            else -> "[behind](${reverseCommitComparisons.permalinkUrl})"
        }
        val branchStatus = "${AppEmojis.changesPush.formatted} ${commitComparisons.aheadBy} commits ahead, ${AppEmojis.changesUpdate.formatted} ${commitComparisons.behindBy} commits $behindText"

        if (jitpackPrService.canUsePullUpdate(libraryType)) {
            val updateButton = when {
                updating -> buttons.primary("Updating...", AppEmojis.sync).toLabelButton()
                pullRequest.mergeable != true || commitComparisons.behindBy == 0 -> buttons.primary(label = "Update PR", emoji = AppEmojis.sync).toLabelButton()
                else -> buttons.primary(label = "Update PR", emoji = AppEmojis.sync).ephemeral {
                    timeout(1.hours)
                    bindTo {
                        onUpdatePrClick(it, targetBranch, additionalDetails)
                    }
                }
            }

            +Section(accessory = updateButton) {
                +TextDisplay(when (pullRequest.mergeable) {
                    true -> branchStatus
                    false -> "$branchStatus, has conflicts"
                    null -> "$branchStatus, checking for conflicts..."
                })
            }
        } else {
            +TextDisplay(branchStatus)
        }

        displayMissingCommits(reverseCommitComparisons)
    }

    private fun InlineContainer.displayMissingCommits(reverseCommitComparisons: CommitComparisons) {
        fun CommitComparisons.Commit.asText(): String {
            return "[`${sha.asSha7}`](${htmlUrl}) ${commit.message.lineSequence().first()} (${TimeFormat.RELATIVE.format(commit.committer.date)})"
        }

        // If the base branch is ahead (PR is missing updates)
        if (reverseCommitComparisons.aheadBy == 1) {
            +TextDisplay(reverseCommitComparisons.commits[0].asText())
        } else if (reverseCommitComparisons.aheadBy == 2) {
            +TextDisplay("""
                |${reverseCommitComparisons.commits[0].asText()}
                |${reverseCommitComparisons.commits[1].asText()}
            """.trimMargin())
        } else if (reverseCommitComparisons.aheadBy > 2) {
            +TextDisplay("""
                |${reverseCommitComparisons.commits.first().asText()}
                |...
                |${reverseCommitComparisons.commits.last().asText()}
            """.trimMargin())
        }
    }

    @AutocompleteHandler(name = PR_NUMBER_AUTOCOMPLETE_NAME, showUserInput = false)
    suspend fun onPRNumberAutocomplete(
        event: CommandAutoCompleteInteractionEvent,
        libraryType: LibraryType
    ): Collection<Choice> {
        return jitpackPrService.getPullRequests(libraryType).toAutocompleteChoices(event)
    }

    context(libraryType: LibraryType, buildToolType: BuildToolType)
    private suspend fun onUpdatePrClick(event: ButtonEvent, targetBranch: UpdatedBranch, additionalDetails: AdditionalPullRequestDetails) {
        val (pullRequest) = additionalDetails

        createPrMessage(event, pullRequest, targetBranch, additionalDetails, updating = true)
            .toEditData()
            .edit(event)
            .queue()

        jitpackPrService.updatePr(
            libraryType,
            pullRequest,
            onMergeConflict = {
                val message = createPrMessage(
                    event, pullRequest, targetBranch,
                    additionalDetails.copy(updatedPR = pullRequest.copy(mergeable = false))
                )
                // Sometimes funny people delete the /jitpack message before the update has finished
                event.hook.editOriginal(message.toEditData()).queueIgnoring(ErrorResponse.UNKNOWN_MESSAGE)
                event.hook.send("Could not update pull request as it has merge conflicts", ephemeral = true).queue()
            },
            onError = {
                val message = createPrMessage(
                    event, pullRequest, targetBranch,
                    additionalDetails.copy(updatedPR = pullRequest.copy(mergeable = false))
                )
                // Sometimes funny people delete the /jitpack message before the update has finished
                event.hook.editOriginal(message.toEditData()).queueIgnoring(ErrorResponse.UNKNOWN_MESSAGE)
                event.hook.send("Could not update pull request", ephemeral = true).queue()
            },
            onSuccess = { branch ->
                val message = createPrMessage(
                    event, pullRequest, branch,
                    additionalDetails.copy(
                        commitComparisons = additionalDetails.commitComparisons.copy(behindBy = 0),
                        reverseCommitComparisons = additionalDetails.reverseCommitComparisons.copy(aheadBy = 0)
                    )
                )

                // Sometimes funny people delete the /jitpack message before the update has finished
                event.hook.editOriginal(message.toEditData()).queueIgnoring(ErrorResponse.UNKNOWN_MESSAGE)
            }
        )
    }

    private fun UpdatedBranch.toJitpackArtifact(): ArtifactInfo = ArtifactInfo(
        "io.github.$ownerName",
        repoName,
        sha.asSha10
    )

    private fun Branch.toUpdatedBranch(): UpdatedBranch = UpdatedBranch(ownerName, repoName, branchName, latestCommitSha)

    companion object {
        const val PR_NUMBER_AUTOCOMPLETE_NAME = "SlashJitpack: prNumber"
    }
}
