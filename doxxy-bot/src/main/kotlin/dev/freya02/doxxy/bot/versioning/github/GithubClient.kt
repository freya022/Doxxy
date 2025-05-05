package dev.freya02.doxxy.bot.versioning.github

import io.github.freya022.botcommands.api.core.service.annotations.BService
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNamingStrategy

// TODO migrate all GH request to here
@BService
class GithubClient(
    client: HttpClient,
) {

    @OptIn(ExperimentalSerializationApi::class)
    private val client = client.config {
        defaultRequest {
            headers {
                header("Accept", "application/vnd.github.v3+json")
            }
        }

        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                namingStrategy = JsonNamingStrategy.SnakeCase
            })
        }
    }

    suspend fun getPullRequest(owner: String, repo: String, pr: Int): PullRequest {
        return client
            .get("https://api.github.com/repos/$owner/$repo/pulls/$pr")
            .body()
    }

    suspend fun compareCommits(owner: String, repo: String, baseLabel: String, headLabel: String): CommitComparisons {
        return client
            .get("https://api.github.com/repos/$owner/$repo/compare/$baseLabel...$headLabel")
            .body()
    }
}