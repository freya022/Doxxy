package dev.freya02.doxxy.bot.utils.github

import dev.freya02.doxxy.bot.utils.Utils.truncate
import dev.freya02.doxxy.github.client.data.PullRequest
import info.debatty.java.stringsimilarity.LongestCommonSubsequence
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.interactions.commands.Command.Choice
import net.dv8tion.jda.api.interactions.commands.build.OptionData

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
        return "$number - $title".truncate(Choice.MAX_NAME_LENGTH - branchAuthorName.length) + branchAuthorName
    }