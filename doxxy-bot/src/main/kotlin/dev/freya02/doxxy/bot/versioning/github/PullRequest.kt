package dev.freya02.doxxy.bot.versioning.github

import dev.freya02.doxxy.bot.utils.Utils.truncate
import info.debatty.java.stringsimilarity.LongestCommonSubsequence
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNamingStrategy
import kotlinx.serialization.json.decodeFromJsonElement
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.interactions.commands.Command
import net.dv8tion.jda.api.interactions.commands.Command.Choice
import net.dv8tion.jda.api.interactions.commands.build.OptionData

@Serializable
data class User(
    val login: String,
)

@Serializable
data class Repo(
    val name: String,
    val owner: User,
)

@Serializable
data class PullRequest(
    val number: Int,
    val title: String,
    val draft: Boolean,
    val user: User,
    val base: Branch,
    val head: Branch,
    val mergeable: Boolean? = null,
    val htmlUrl: String,
) {

    val authorName get() = user.login
    val pullUrl get() = htmlUrl
    val branch get() = head

    companion object {

        @OptIn(ExperimentalSerializationApi::class)
        private val json = Json {
            ignoreUnknownKeys = true
            namingStrategy = JsonNamingStrategy.SnakeCase
        }

        fun fromData(jsonElement: JsonElement): PullRequest {
            return json.decodeFromJsonElement<PullRequest>(jsonElement)
        }
    }
}

private val lcs = LongestCommonSubsequence()

fun Collection<PullRequest>.toAutocompleteChoices(event: CommandAutoCompleteInteractionEvent): List<Choice> {
    val query = event.focusedOption.value
    val sortedPullRequests = when {
        query.isBlank() -> sortedByDescending { it.number }

        query.first().isDigit() ->
            filter { it.number.toString().startsWith(query) }
                .sortedByDescending { it.number }

        else -> {
            val lowercaseQuery = query.lowercase()
            sortedByDescending { lcs.length(lowercaseQuery, it.title.lowercase()) }
        }
    }.take(OptionData.MAX_CHOICES)

    return sortedPullRequests.map { it.toChoice() }
}

private fun PullRequest.toChoice() = Choice(asHumanDescription, number.toLong())

private val PullRequest.asHumanDescription: String
    get() {
        val branchAuthorName = " ($authorName)"
        return "$number - $title".truncate(Command.Choice.MAX_NAME_LENGTH - branchAuthorName.length) + branchAuthorName
    }