package com.freya02.bot.commands.slash.versioning

import com.freya02.bot.commands.slash.DeleteButtonListener.Companion.messageDelete
import com.freya02.bot.utils.Emojis
import com.freya02.bot.utils.Utils.isBCGuild
import com.freya02.bot.utils.Utils.truncate
import com.freya02.bot.versioning.LibraryType
import com.freya02.bot.versioning.ScriptType
import com.freya02.bot.versioning.github.GithubBranch
import com.freya02.bot.versioning.github.PullRequest
import com.freya02.bot.versioning.github.PullRequest.Companion.toAutocompleteChoices
import com.freya02.bot.versioning.jitpack.JitpackBranchService
import com.freya02.bot.versioning.jitpack.JitpackPrService
import com.freya02.bot.versioning.jitpack.pullupdater.PullUpdater
import com.freya02.bot.versioning.supplier.BuildToolType
import com.freya02.bot.versioning.supplier.DependencySupplier
import dev.minn.jda.ktx.coroutines.await
import dev.minn.jda.ktx.interactions.components.link
import dev.minn.jda.ktx.interactions.components.row
import dev.minn.jda.ktx.messages.Embed
import dev.minn.jda.ktx.messages.MessageCreate
import dev.minn.jda.ktx.messages.reply_
import dev.minn.jda.ktx.messages.send
import io.github.freya022.botcommands.api.annotations.CommandMarker
import io.github.freya022.botcommands.api.commands.annotations.Command
import io.github.freya022.botcommands.api.commands.application.ApplicationCommand
import io.github.freya022.botcommands.api.commands.application.CommandScope
import io.github.freya022.botcommands.api.commands.application.provider.GuildApplicationCommandManager
import io.github.freya022.botcommands.api.commands.application.provider.GuildApplicationCommandProvider
import io.github.freya022.botcommands.api.commands.application.slash.GuildSlashEvent
import io.github.freya022.botcommands.api.commands.application.slash.autocomplete.AutocompleteAlgorithms
import io.github.freya022.botcommands.api.commands.application.slash.autocomplete.annotations.AutocompleteHandler
import io.github.freya022.botcommands.api.commands.application.slash.builder.SlashCommandBuilder
import io.github.freya022.botcommands.api.components.Buttons
import io.github.freya022.botcommands.api.components.event.ButtonEvent
import io.github.freya022.botcommands.api.core.utils.runIgnoringResponse
import io.github.freya022.botcommands.api.core.utils.toEditData
import io.github.freya022.botcommands.api.utils.EmojiUtils
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.events.interaction.GenericInteractionCreateEvent
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.interactions.commands.Command.Choice
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import net.dv8tion.jda.api.interactions.components.ItemComponent
import net.dv8tion.jda.api.requests.ErrorResponse
import net.dv8tion.jda.api.utils.messages.MessageCreateData
import kotlin.time.Duration.Companion.hours

@Command
class SlashJitpack(
    private val buttons: Buttons,
    private val jitpackPrService: JitpackPrService,
    private val jitpackBranchService: JitpackBranchService
) : ApplicationCommand(), GuildApplicationCommandProvider {
    @CommandMarker
    suspend fun onSlashJitpackPR(event: GuildSlashEvent, libraryType: LibraryType, buildToolType: BuildToolType, pullNumber: Int) {
        val pullRequest = jitpackPrService.getPullRequest(libraryType, pullNumber)
            ?: return event.reply_("Unknown Pull Request", ephemeral = true).queue()

        val message = createPrMessage(event, libraryType, buildToolType, pullRequest, pullRequest.branch)

        event.reply(message).queue()
    }

    private suspend fun createPrMessage(
        event: GenericInteractionCreateEvent,
        libraryType: LibraryType,
        buildToolType: BuildToolType,
        pullRequest: PullRequest,
        targetBranch: GithubBranch
    ): MessageCreateData {
        val dependencyStr: String = when (libraryType) {
            LibraryType.BOT_COMMANDS -> DependencySupplier.formatBCJitpack(
                ScriptType.DEPENDENCIES,
                buildToolType,
                jitpackBranchService.getUsedJDAVersionFromBranch(LibraryType.BOT_COMMANDS, targetBranch),
                targetBranch.toJitpackArtifact()
            )
            LibraryType.JDA, LibraryType.JDA_KTX, LibraryType.LAVA_PLAYER -> DependencySupplier.formatJitpack(
                ScriptType.DEPENDENCIES,
                buildToolType,
                targetBranch.toJitpackArtifact()
            )
        }

        return MessageCreate {
            embed {
                title = "${buildToolType.humanName} dependencies for ${libraryType.displayString}: ${pullRequest.title}"
                    .truncate(MessageEmbed.TITLE_MAX_LENGTH)
                url = pullRequest.pullUrl

                field("PR Link", pullRequest.pullUrl, false)

                description = when (buildToolType) {
                    BuildToolType.MAVEN -> "```xml\n$dependencyStr```"
                    BuildToolType.GRADLE, BuildToolType.GRADLE_KTS -> "```gradle\n$dependencyStr```"
                }

                if (jitpackPrService.canUsePullUpdate(libraryType)) {
                    description += "\nYou can also click on the `Update PR` button to merge the latest changes."
                }
            }

            components += buildList<ItemComponent>(3) {
                if (jitpackPrService.canUsePullUpdate(libraryType)) {
                    this += buttons.primary(label = "Update PR", emoji = Emojis.sync).ephemeral {
                        val callerId = event.user.idLong
                        timeout(1.hours)
                        bindTo {
                            onUpdatePrClick(it, callerId, libraryType, buildToolType, pullRequest.number)
                        }
                    }
                }
                this += link(
                    "https://jda.wiki/using-jda/using-new-features/",
                    "How? (Wiki)",
                    EmojiUtils.resolveJDAEmoji("face_with_monocle")
                )
                this += buttons.messageDelete(event.user)
            }.row()
        }
    }

    @AutocompleteHandler(name = PR_NUMBER_AUTOCOMPLETE_NAME, showUserInput = false)
    fun onPRNumberAutocomplete(
        event: CommandAutoCompleteInteractionEvent,
        libraryType: LibraryType
    ): Collection<Choice> {
        return jitpackPrService.getPullRequests(libraryType).toAutocompleteChoices(event)
    }

    private suspend fun onUpdatePrClick(event: ButtonEvent, callerId: Long, libraryType: LibraryType, buildToolType: BuildToolType, pullNumber: Int) {
        val pullRequest = jitpackPrService.getPullRequest(libraryType, pullNumber)
            ?: return event.reply_("Unknown Pull Request", ephemeral = true).queue()

        event.deferEdit().queue()
        val waitMessage = when {
            PullUpdater.isRunning -> "Please wait while the pull request is being updated, this may be longer than usual"
            else -> "Please wait while the pull request is being updated"
        }.let { event.hook.send(it, ephemeral = true).await() }

        // Sometimes funny people delete the /jitpack message before the update has finished
        runIgnoringResponse(ErrorResponse.UNKNOWN_MESSAGE) {
            jitpackPrService.updatePr(libraryType, pullNumber, event.hook, waitMessage.idLong) { branch ->
                val message = createPrMessage(event, libraryType, buildToolType, pullRequest, branch)
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
        manager.slashCommand("jitpack", CommandScope.GUILD, null) {
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
                jitpackBranchService.getUsedJDAVersionFromBranch(LibraryType.BOT_COMMANDS, branch),
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
            .addActionRow(buttons.messageDelete(event.user))
            .queue()
    }

    companion object {
        const val PR_NUMBER_AUTOCOMPLETE_NAME = "SlashJitpack: prNumber"
        private const val BRANCH_NAME_AUTOCOMPLETE_NAME = "SlashJitpack: branchName"
    }
}
