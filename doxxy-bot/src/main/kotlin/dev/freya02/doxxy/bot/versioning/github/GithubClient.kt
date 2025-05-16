package dev.freya02.doxxy.bot.versioning.github

import io.github.freya022.botcommands.api.core.service.annotations.BService
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNamingStrategy

// TODO migrate all GH request to here
@BService
class GithubClient(
    defaultClient: HttpClient,
) {

    @OptIn(ExperimentalSerializationApi::class)
    private val client = defaultClient.config {
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

    fun getBranches(owner: String, repo: String, perPage: Int = 100): Flow<Branches.Branch> {
        return withPagination(perPage, "https://api.github.com/repos/$owner/$repo/branches", fetch = HttpResponse::body)
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

    // Pagination endpoints often have different schemas than individual GETs, so we make different DTOs for those,
    // this is why there is a kind-of namespace interface like [[Branches]], containing [[Branches#Branch]]
    private fun <R> withPagination(
        perPage: Int,
        url: String,
        customizer: HttpRequestBuilder.() -> Unit = {},
        fetch: suspend (HttpResponse) -> List<R>,
    ): Flow<R> = flow {
        var page = 0

        while (true) {
            val items = fetch(client.get(url) {
                customizer()

                url {
                    parameter("page", page)
                    parameter("per_page", perPage)
                }
            })

            items.forEach { emit(it) }
            // Last items
            if (items.size < perPage) break

            page += 1
        }
    }
}