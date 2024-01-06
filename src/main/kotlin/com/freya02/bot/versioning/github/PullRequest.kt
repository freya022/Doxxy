package com.freya02.bot.versioning.github

import com.freya02.bot.utils.Utils.truncate
import com.freya02.bot.versioning.ArtifactInfo
import net.dv8tion.jda.api.interactions.commands.Command
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
    }
}