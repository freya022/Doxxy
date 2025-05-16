package dev.freya02.doxxy.bot.versioning.github

import dev.freya02.doxxy.bot.utils.HttpUtils
import io.github.oshai.kotlinlogging.KotlinLogging
import net.dv8tion.jda.api.utils.data.DataArray
import net.dv8tion.jda.api.utils.data.DataObject
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request

private typealias URLBuilder = HttpUrl.Builder
private typealias RequestBuilder = Request.Builder

object GithubUtils {
    private val logger = KotlinLogging.logger { }

    private fun newGithubRequest(url: HttpUrl): RequestBuilder {
        return RequestBuilder()
            .url(url)
            .header("Accept", "application/vnd.github.v3+json")
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