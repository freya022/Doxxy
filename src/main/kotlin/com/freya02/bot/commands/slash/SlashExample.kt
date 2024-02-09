package com.freya02.bot.commands.slash

import com.freya02.bot.switches.RequiresBackend
import com.freya02.bot.utils.Utils.isBCGuild
import com.freya02.bot.utils.Utils.letIf
import dev.minn.jda.ktx.messages.reply_
import dev.minn.jda.ktx.messages.send
import io.github.freya022.botcommands.api.commands.annotations.Command
import io.github.freya022.botcommands.api.commands.application.ApplicationCommand
import io.github.freya022.botcommands.api.commands.application.CommandScope
import io.github.freya022.botcommands.api.commands.application.annotations.Test
import io.github.freya022.botcommands.api.commands.application.slash.GuildSlashEvent
import io.github.freya022.botcommands.api.commands.application.slash.annotations.JDASlashCommand
import io.github.freya022.botcommands.api.commands.application.slash.annotations.SlashOption
import io.github.freya022.botcommands.api.commands.application.slash.annotations.TopLevelSlashCommandData
import io.github.freya022.botcommands.api.commands.application.slash.autocomplete.annotations.AutocompleteHandler
import io.github.freya022.botcommands.api.commands.application.slash.autocomplete.annotations.CacheAutocomplete
import io.github.freya022.botcommands.api.core.service.annotations.ServiceName
import io.github.freya022.botcommands.api.core.utils.deleteDelayed
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlinx.serialization.Serializable
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.interactions.commands.Command.Choice
import kotlin.time.Duration.Companion.seconds

private const val titleAutocompleteName = "SlashExample: title"

@RequiresBackend
@Command
class SlashExample(
    @ServiceName("backendClient") private val backendClient: HttpClient
) : ApplicationCommand() {
    //TODO add to common module
    @Serializable
    data class ExampleDTO(
        val title: String,
        val library: String,
        val contents: List<ExampleContentDTO>
    ) {
        @Serializable
        data class ExampleContentDTO(
            val language: String,
            val content: String
        )
    }

    @Serializable
    data class ExampleSearchResultDTO(
        val title: String,
        val library: String
    )

    @JDASlashCommand(name = "example", description = "No description")
    suspend fun onSlashExample(
        event: GuildSlashEvent,
        @SlashOption(
            description = "The title of the requested example",
            autocomplete = titleAutocompleteName
        ) title: String
    ) {
        val example = backendClient.get("example?title=$title").body<ExampleDTO?>()
            ?: return event.reply_("No example found", ephemeral = true).queue()

        event.reply_(example.contents.first().content).queue()
    }

    @CacheAutocomplete
    @AutocompleteHandler(titleAutocompleteName, showUserInput = false)
    suspend fun onTitleAutocomplete(event: CommandAutoCompleteInteractionEvent): List<Choice> {
        val titleQuery = event.focusedOption.value
        val examples = backendClient
            .get("examples/search?query=$titleQuery")
            .body<List<ExampleSearchResultDTO>>()
            .letIf(event.guild.isBCGuild()) { it.filter { example -> example.library != "BotCommands" } }

        return examples.map { Choice("${it.library} - ${it.title}", it.title) }
    }

    @Test
    @TopLevelSlashCommandData(scope = CommandScope.GUILD, description = "Manage examples")
    @JDASlashCommand(name = "examples", subcommand = "update", description = "Fetch latest examples")
    suspend fun onSlashExamplesUpdate(event: GuildSlashEvent) {
        event.deferReply(true).queue()

        backendClient.put("examples/update")

        event.hook.send("Done!")
            .deleteDelayed(5.seconds)
            .queue()
    }
}