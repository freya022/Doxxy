package dev.freya02.doxxy.bot.versioning.maven

import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

class MavenRepositoryClient(
    private val baseUrl: String,
) {

    private val client = HttpClient(OkHttp)

    // Do not trust the "latest" or "release" attribute of maven-metadata.xml
    // as it does not correspond to the latest-in-date release
    // This is why here we parse the FTP-style page and find using the most recent date
    suspend fun getContents(groupId: String, artifactId: String): Document {
        val response = client.get(baseUrl) {
            url {
                appendPathSegments(groupId.replace('.', '/'), artifactId)
            }
        }

        return Jsoup.parse(response.bodyAsText(), response.request.url.toString())
    }
}

private val contentRegex = Regex("""^(.+)/\s+(\d{4}-\d{2}-\d{2})""")
suspend fun MavenRepositoryClient.getLatestVersion(groupId: String, artifactId: String): String {
    val contentsDoc = getContents(groupId, artifactId)
    val contents = contentsDoc.selectFirst("html > body > main > pre#contents")!!.text()
    val latestVersion = contents.lines()
        .mapNotNull { contentRegex.find(it) }
        .map { it.groupValues }
        .maxBy { (_, _, date) -> date }[1]

    return latestVersion
}
