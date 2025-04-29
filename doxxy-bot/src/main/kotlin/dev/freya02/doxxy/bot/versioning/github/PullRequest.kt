package dev.freya02.doxxy.bot.versioning.github

import dev.freya02.doxxy.bot.utils.Utils.truncate
import dev.freya02.doxxy.bot.versioning.ArtifactInfo
import info.debatty.java.stringsimilarity.LongestCommonSubsequence
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.interactions.commands.Command
import net.dv8tion.jda.api.interactions.commands.Command.Choice
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import net.dv8tion.jda.api.utils.data.DataObject

@JvmRecord
data class PullRequest(
    val number: Int,
    val title: String,
    val draft: Boolean,
    val authorName: String,
    val branch: GithubBranch,
    val pullUrl: String
) {
    fun toJitpackArtifact(): ArtifactInfo {
        return branch.toJitpackArtifact()
    }

    val asHumanDescription: String
        get() {
            val branchAuthorName = " ($authorName)"
            return "$number - $title".truncate(Command.Choice.MAX_NAME_LENGTH - branchAuthorName.length) + branchAuthorName
        }

    companion object {
        private val lcs = LongestCommonSubsequence()

        fun fromData(data: DataObject): PullRequest? {
            val head = data.getObject("head")
            if (head.isNull("repo")) { //If we don't have that then the head repo (the fork) doesn't exist
                return null
            }

            val headRepo = head.getObject("repo")
            val number = data.getInt("number")
            val title = data.getString("title")
            val draft = data.getBoolean("draft")
            val authorName = data.getObject("user").getString("login")
            val headRepoOwnerName = head.getObject("user").getString("login")
            val headRepoName = headRepo.getString("name")
            val headBranchName = head.getString("ref")
            val latestHash = head.getString("sha")
            val pullUrl = data.getString("html_url")

            return PullRequest(
                number,
                title,
                draft,
                authorName,
                GithubBranch(headRepoOwnerName, headRepoName, headBranchName, CommitHash(latestHash)),
                pullUrl
            )
        }

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
    }
}