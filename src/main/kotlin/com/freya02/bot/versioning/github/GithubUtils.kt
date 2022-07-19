package com.freya02.bot.versioning.github

import com.freya02.bot.utils.HttpUtils
import com.freya02.botcommands.api.Logging
import gnu.trove.map.TIntObjectMap
import gnu.trove.map.hash.TIntObjectHashMap
import net.dv8tion.jda.api.utils.data.DataArray
import net.dv8tion.jda.api.utils.data.DataObject
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import java.io.IOException

private typealias URLBuilder = HttpUrl.Builder
private typealias RequestBuilder = Request.Builder

object GithubUtils {
    private val LOGGER = Logging.getLogger()

    private fun newGithubRequest(url: HttpUrl): RequestBuilder {
        return RequestBuilder()
            .url(url)
            .header("Accept", "applications/vnd.github.v3+json")
    }

    @Throws(IOException::class)
    fun getLatestBranch(ownerName: String, artifactId: String): GithubBranch {
        val branches = getBranches(ownerName, artifactId)
        return branches
            .filter { s: GithubBranch -> s.branchName.matches("\\d\\.\\d\\.\\d".toRegex()) }
            .maxWithOrNull(Comparator.comparing { obj: GithubBranch -> obj.branchName }) ?: branches[0]
    }

    @Throws(IOException::class)
    fun getLatestHash(ownerName: String, repoName: String, branchName: String): String {
        val url = "https://api.github.com/repos/$ownerName/$repoName/commits".toHttpUrl()
            .newBuilder()
            .addQueryParameter("page", "1")
            .addQueryParameter("per_page", "1")
            .addQueryParameter("sha", branchName)
            .build()

        HttpUtils.CLIENT.newCall(newGithubRequest(url).build())
            .execute()
            .use { response ->
                val json = response.body!!.string()
                return DataArray.fromJson(json).getObject(0).getString("sha").substring(0, 10)
            }
    }

    @Throws(IOException::class)
    fun getDefaultBranchName(ownerName: String, repoName: String): String {
        val url = "https://api.github.com/repos/$ownerName/$repoName".toHttpUrl()
            .newBuilder()
            .build()
        HttpUtils.CLIENT.newCall(
            newGithubRequest(url)
                .build()
        )
            .execute().use { response ->
                val json = response.body!!.string()
                val dataObject = DataObject.fromJson(json)
                return dataObject.getString("default_branch")
            }
    }

    @Throws(IOException::class)
    fun getBranches(ownerName: String, repoName: String): List<GithubBranch> {
        val url = "https://api.github.com/repos/$ownerName/$repoName/branches".toHttpUrl()
            .newBuilder()
            .addQueryParameter("page", "1")
            .addQueryParameter("per_page", "30")
            .build()
        HttpUtils.CLIENT.newCall(newGithubRequest(url).build())
            .execute()
            .use { response ->
                val json = response.body!!.string()
                val branches = DataArray.fromJson(json)

                return (0 until branches.length()).map { i ->
                    val branchObject = branches.getObject(i)
                    val name = branchObject.getString("name")
                    val sha = branchObject.getObject("commit").getString("sha")

                    GithubBranch(ownerName, repoName, name, CommitHash(sha))
                }
            }
    }

    @Throws(IOException::class)
    fun retrievePullRequests(
        ownerName: String,
        artifactId: String,
        baseBranchName: String?
    ): TIntObjectMap<PullRequest> {
        LOGGER.debug("Retrieving pull requests of {}/{}", ownerName, artifactId)

        val pullRequests: TIntObjectMap<PullRequest> = TIntObjectHashMap()
        val urlBuilder: URLBuilder = "https://api.github.com/repos/$ownerName/$artifactId/pulls".toHttpUrl()
                .newBuilder()
                .addQueryParameter("page", "1")
                .addQueryParameter("per_page", "30")

        if (baseBranchName != null) {
            urlBuilder.addQueryParameter("base", baseBranchName)
        }

        HttpUtils.CLIENT.newCall(newGithubRequest(urlBuilder.build()).build())
            .execute()
            .use { response ->
                val json = response.body!!.string()
                val array = DataArray.fromJson(json)

                (0 until array.length()).forEach { i ->
                    val pullRequest = PullRequest.fromData(array.getObject(i))
                    if (pullRequest != null) {
                        pullRequests.put(pullRequest.number, pullRequest)
                    }
                }
            }

        return pullRequests
    }

    fun getLatestRelease(ownerName: String, repoName: String): GithubRelease? {
        val url = "https://api.github.com/repos/$ownerName/$repoName/releases/latest".toHttpUrl()

        return HttpUtils.doRequest(newGithubRequest(url).build(), false) { response, body ->
            if (!response.isSuccessful) return@doRequest null

            val obj = DataObject.fromJson(body.string())
            GithubRelease(obj.getString("tag_name"))
        }
    }

    fun getAllTags(ownerName: String, repoName: String): List<GithubTag> {
        val url = "https://api.github.com/repos/$ownerName/$repoName/tags".toHttpUrl()

        return HttpUtils.doRequest(newGithubRequest(url).build()) { _, body ->
            val array = DataArray.fromJson(body.string())
            (0 until array.length()).map { i ->
                val obj = array.getObject(i)
                GithubTag(obj.getString("name"), CommitHash(obj.getObject("commit").getString("sha")))
            }
        }
    }

    fun getLatestReleaseHash(ownerName: String, repoName: String): CommitHash? {
        return getLatestRelease(ownerName, repoName)?.let { release ->
            getAllTags(ownerName, repoName).find { tag -> tag.name == release.tagName }?.commitHash
        }
    }
}