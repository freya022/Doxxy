package dev.freya02.doxxy.bot.commands.slash

import dev.freya02.doxxy.bot.examples.ExampleAPI
import dev.freya02.doxxy.bot.examples.ExamplePaginatorFactory
import dev.freya02.doxxy.bot.switches.RequiresBackend
import dev.freya02.doxxy.bot.utils.Utils.isBCGuild
import dev.freya02.doxxy.bot.utils.Utils.letIf
import dev.freya02.doxxy.bot.versioning.github.PullRequestCache
import dev.freya02.doxxy.bot.versioning.github.toAutocompleteChoices
import dev.minn.jda.ktx.messages.reply_
import dev.minn.jda.ktx.messages.send
import io.github.freya022.botcommands.api.annotations.CommandMarker
import io.github.freya022.botcommands.api.commands.annotations.Command
import io.github.freya022.botcommands.api.commands.application.ApplicationCommand
import io.github.freya022.botcommands.api.commands.application.provider.GuildApplicationCommandManager
import io.github.freya022.botcommands.api.commands.application.provider.GuildApplicationCommandProvider
import io.github.freya022.botcommands.api.commands.application.slash.GuildSlashEvent
import io.github.freya022.botcommands.api.commands.application.slash.annotations.JDASlashCommand
import io.github.freya022.botcommands.api.commands.application.slash.annotations.SlashOption
import io.github.freya022.botcommands.api.commands.application.slash.autocomplete.annotations.AutocompleteHandler
import io.github.freya022.botcommands.api.commands.application.slash.autocomplete.annotations.CacheAutocomplete
import io.github.freya022.botcommands.api.core.utils.deleteDelayed
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.interactions.commands.Command.Choice
import kotlin.time.Duration.Companion.seconds

private const val titleAutocompleteName = "SlashExample: title"
private const val languageAutocompleteName = "SlashExample: language"
private const val pullRequestNumberAutocompleteName = "SlashExample: pullRequestNumber"

@RequiresBackend
@Command
class SlashExample(
    private val exampleApi: ExampleAPI,
    private val paginatorFactory: ExamplePaginatorFactory
) : ApplicationCommand(), GuildApplicationCommandProvider {

    @JDASlashCommand(name = "example", description = "Sends an example")
    suspend fun onSlashExample(
        event: GuildSlashEvent,
        @SlashOption(
            description = "The title of the requested example",
            autocomplete = titleAutocompleteName
        ) title: String,
        @SlashOption(
            description = "The language of the requested example",
            autocomplete = languageAutocompleteName
        )
        language: String
    ) {
        val example = exampleApi.getExampleByTitle(title)
            ?: return event.reply_("This example does not exist", ephemeral = true).queue()
        val contentDTO = example.contents.firstOrNull { it.language == language }
            ?: return event.reply_("This language is not available", ephemeral = true).queue()

        val paginator = paginatorFactory.fromInteraction(contentDTO.parts, event.user, ephemeral = false, event.hook)
        event.reply(paginator.createMessage()).queue()
    }

    @CacheAutocomplete
    @AutocompleteHandler(titleAutocompleteName, showUserInput = false)
    suspend fun onTitleAutocomplete(event: CommandAutoCompleteInteractionEvent): List<Choice> {
        val titleQuery = event.focusedOption.value
        val examples = exampleApi.searchExamplesByTitle(titleQuery)
            .letIf(event.guild.isBCGuild()) { it.filter { example -> example.library != "BotCommands" } }

        return examples.map { Choice("${it.library} - ${it.title}", it.title) }
    }

    @CacheAutocomplete(compositeKeys = ["title", "language"])
    @AutocompleteHandler(languageAutocompleteName, showUserInput = false)
    suspend fun onLanguageAutocomplete(event: CommandAutoCompleteInteractionEvent, title: String): List<Choice> {
        return exampleApi.getLanguagesByTitle(title).map { Choice(it, it) }
    }

    override fun declareGuildApplicationCommands(manager: GuildApplicationCommandManager) {
        if (manager.guild.idLong !in manager.context.applicationConfig.testGuildIds) return

        manager.slashCommand("examples", function = null) {
            description = "Manage examples"

            subcommand("update", ::onSlashExamplesUpdateFromPR) {
                description = "Fetch latest examples"

                option("pullRequestNumber", "pull_request") {
                    description = "The pull request to load the examples from"
                    autocompleteByName(pullRequestNumberAutocompleteName)
                }
            }
        }
    }

    private val pullRequestCache by lazy {
        PullRequestCache(githubOwnerName = "freya022", githubRepoName = "doc-examples", baseBranchName = "master")
    }

    @CommandMarker
    suspend fun onSlashExamplesUpdateFromPR(event: GuildSlashEvent, pullRequestNumber: Int?) {
        if (pullRequestNumber != null) {
            val pr = pullRequestCache.pullRequests[pullRequestNumber]
            onSlashExamplesUpdate(event, pr.branch.ownerName, pr.branch.repoName, pr.branch.branchName)
        } else {
            onSlashExamplesUpdate(event)
        }
    }

    @AutocompleteHandler(pullRequestNumberAutocompleteName, showUserInput = false)
    fun onPullRequestNumberAutocomplete(event: CommandAutoCompleteInteractionEvent): List<Choice> {
        return pullRequestCache.pullRequests.valueCollection().toAutocompleteChoices(event)
    }

    private suspend fun onSlashExamplesUpdate(event: GuildSlashEvent, ownerName: String = "freya022", repoName: String = "doc-examples", branchName: String = "master") {
        event.deferReply(true).queue()

        if (exampleApi.updateExamples(ownerName, repoName, branchName)) {
            event.context.applicationCommandsContext.invalidateAutocompleteCache(titleAutocompleteName)
            event.context.applicationCommandsContext.invalidateAutocompleteCache(languageAutocompleteName)

            event.hook.send("Done!")
                .deleteDelayed(5.seconds)
                .queue()
        } else {
            event.hook.send("Unable to update examples").queue()
        }
    }
}