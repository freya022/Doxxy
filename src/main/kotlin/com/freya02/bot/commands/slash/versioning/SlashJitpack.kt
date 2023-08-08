package com.freya02.bot.commands.slash.versioning

import com.freya02.bot.Config
import com.freya02.bot.commands.slash.DeleteButtonListener.Companion.messageDeleteButton
import com.freya02.bot.utils.Emojis
import com.freya02.bot.utils.Utils.isBCGuild
import com.freya02.bot.utils.Utils.truncate
import com.freya02.bot.versioning.LibraryType
import com.freya02.bot.versioning.ScriptType
import com.freya02.bot.versioning.github.GithubBranch
import com.freya02.bot.versioning.github.PullRequest
import com.freya02.bot.versioning.jitpack.JitpackBranchService
import com.freya02.bot.versioning.jitpack.JitpackPrService
import com.freya02.bot.versioning.supplier.BuildToolType
import com.freya02.bot.versioning.supplier.DependencySupplier
import com.freya02.botcommands.api.annotations.CommandMarker
import com.freya02.botcommands.api.commands.annotations.Command
import com.freya02.botcommands.api.commands.application.ApplicationCommand
import com.freya02.botcommands.api.commands.application.CommandScope
import com.freya02.botcommands.api.commands.application.GuildApplicationCommandManager
import com.freya02.botcommands.api.commands.application.annotations.AppDeclaration
import com.freya02.botcommands.api.commands.application.slash.GuildSlashEvent
import com.freya02.botcommands.api.commands.application.slash.autocomplete.AutocompleteAlgorithms
import com.freya02.botcommands.api.commands.application.slash.autocomplete.FuzzyResult
import com.freya02.botcommands.api.commands.application.slash.autocomplete.ToStringFunction
import com.freya02.botcommands.api.commands.application.slash.autocomplete.annotations.AutocompleteHandler
import com.freya02.botcommands.api.commands.application.slash.builder.SlashCommandBuilder
import com.freya02.botcommands.api.components.Components
import com.freya02.botcommands.api.components.event.ButtonEvent
import com.freya02.botcommands.api.core.utils.toEditData
import com.freya02.botcommands.api.utils.EmojiUtils
import dev.minn.jda.ktx.interactions.components.link
import dev.minn.jda.ktx.interactions.components.row
import dev.minn.jda.ktx.messages.Embed
import dev.minn.jda.ktx.messages.MessageCreate
import dev.minn.jda.ktx.messages.reply_
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.events.interaction.GenericInteractionCreateEvent
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.interactions.commands.Command.Choice
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import net.dv8tion.jda.api.interactions.components.ItemComponent
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle
import net.dv8tion.jda.api.utils.messages.MessageCreateData
import kotlin.time.Duration.Companion.hours

@Command
class SlashJitpack(
    private val componentsService: Components,
    private val jitpackPrService: JitpackPrService,
    private val jitpackBranchService: JitpackBranchService,
    private val config: Config
) : ApplicationCommand() {
    @CommandMarker
    fun onSlashJitpackPR(event: GuildSlashEvent, libraryType: LibraryType, buildToolType: BuildToolType, pullNumber: Int) {
        val pullRequest = jitpackPrService.getPullRequest(libraryType, pullNumber)
            ?: return event.reply_("Unknown Pull Request", ephemeral = true).queue()

        val message = createPrMessage(event, libraryType, buildToolType, pullRequest, pullRequest.branch)

        event.reply(message).queue()
    }

    private fun createPrMessage(
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
            embed {
                title = "${buildToolType.humanName} dependencies for ${libraryType.displayString}: ${pullRequest.title}"
                    .truncate(MessageEmbed.TITLE_MAX_LENGTH)
                url = pullRequest.pullUrl

                field("PR Link", pullRequest.pullUrl, false)

                description = when (buildToolType) {
                    BuildToolType.MAVEN -> "```xml\n$dependencyStr```"
                    BuildToolType.GRADLE, BuildToolType.GRADLE_KTS -> "```gradle\n$dependencyStr```"
                } + "\nYou can also click on the `Update PR` button to merge the latest changes."
            }

            components += buildList<ItemComponent>(3) {
                if (libraryType == LibraryType.JDA && config.usePullUpdater) {
                    this += componentsService.ephemeralButton(ButtonStyle.PRIMARY, label = "Update PR", emoji = Emojis.sync) {
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
                this += componentsService.messageDeleteButton(event.user)
            }.row()
        }
    }

    @AutocompleteHandler(name = PR_NUMBER_AUTOCOMPLETE_NAME, showUserInput = false)
    fun onPRNumberAutocomplete(
        event: CommandAutoCompleteInteractionEvent,
        libraryType: LibraryType
    ): Collection<Choice> {
        val pullRequests = jitpackPrService.getPullRequests(libraryType)

        return when {
            event.focusedOption.value.isBlank() -> pullRequests.sortedByDescending { it.number }
            else -> pullRequests.fuzzyMatching(
                //Don't autocomplete based on the branch number
                toStringFunction = { referent: PullRequest -> referent.title + referent.branch.authorName },
                query = event.focusedOption.value
            ).map { fuzzyResult -> fuzzyResult.item }
        }.map { r -> r.toChoice() }
    }

    private val mutex = Mutex()
    private suspend fun onUpdatePrClick(event: ButtonEvent, callerId: Long, libraryType: LibraryType, buildToolType: BuildToolType, pullNumber: Int) {
        if (mutex.isLocked)
            return event.reply_("A pull request is already being updated", ephemeral = true).queue()

        val pullRequest = jitpackPrService.getPullRequest(libraryType, pullNumber)
            ?: return event.reply_("Unknown Pull Request", ephemeral = true).queue()

        mutex.withLock {
            jitpackPrService.updatePr(event, pullNumber) { branch ->
                val message = createPrMessage(event, libraryType, buildToolType, pullRequest, branch.toGithubBranch())
                if (event.user.idLong == callerId) {
                    event.hook.editOriginal(message.toEditData()).queue()
                } else {
                    event.hook.sendMessage(message).setEphemeral(true).queue()
                }
            }
        }
    }

    @AutocompleteHandler(name = BRANCH_NAME_AUTOCOMPLETE_NAME, showUserInput = false)
    fun onBranchNameAutocomplete(
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

    @AppDeclaration
    fun declare(manager: GuildApplicationCommandManager) {
        manager.slashCommand("jitpack", CommandScope.GUILD, null) {
            description = "Shows you how to use jitpack for your bot"

            subcommand("branch", SlashJitpack::onSlashJitpackBranch) {
                description = "Shows you how to use a branch for your bot"

                addCommonJitpackOptions(manager)
                option("branchName") {
                    description = "The name of the Git branch to build from"
                    autocompleteReference(BRANCH_NAME_AUTOCOMPLETE_NAME)
                }
            }

            subcommand("pr", SlashJitpack::onSlashJitpackPR) {
                description = "Shows you how to use Pull Requests for your bot"

                addCommonJitpackOptions(manager)
                option("pullNumber") {
                    description = "The Pull Request number"
                    autocompleteReference(PR_NUMBER_AUTOCOMPLETE_NAME)
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
            .addActionRow(componentsService.messageDeleteButton(event.user))
            .queue()
    }

    companion object {
        const val PR_NUMBER_AUTOCOMPLETE_NAME = "SlashJitpack: prNumber"
        private const val BRANCH_NAME_AUTOCOMPLETE_NAME = "SlashJitpack: branchName"

        private fun PullRequest.toChoice() = Choice(asHumanDescription, number.toLong())

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
