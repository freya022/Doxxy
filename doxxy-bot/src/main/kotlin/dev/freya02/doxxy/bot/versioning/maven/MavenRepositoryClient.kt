package dev.freya02.doxxy.bot.versioning.maven

import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import net.dv8tion.jda.api.exceptions.ParsingException
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

class MavenRepositoryClient(
    private val baseUrl: String,
) {

    private val client = HttpClient(OkHttp)

    suspend fun getMavenMetadata(groupId: String, artifactId: String): Document {
        val response = client.get(baseUrl) {
            url {
                appendPathSegments(groupId.replace('.', '/'), artifactId, "maven-metadata.xml")
            }
        }

        return Jsoup.parse(response.bodyAsText(), response.request.url.toString())
    }
}

suspend fun MavenRepositoryClient.getLatestStableMavenVersion(groupId: String, artifactId: String): String {
    val mavenMetadata = getMavenMetadata(groupId, artifactId)
    val latest = mavenMetadata.selectFirst("metadata > versioning > latest")
    return latest?.text() ?: throw ParsingException("Unable to parse latest version")
}