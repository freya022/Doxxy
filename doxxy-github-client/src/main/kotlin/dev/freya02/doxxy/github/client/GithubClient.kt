package dev.freya02.doxxy.github.client

import dev.freya02.doxxy.github.client.data.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNamingStrategy

class GithubClient(
    defaultClient: HttpClient,
    token: String?,
) {

    @OptIn(ExperimentalSerializationApi::class)
    private val client = defaultClient.config {
        defaultRequest {
            headers {
                if (token != null)
                    bearerAuth(token)
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

    suspend fun getRepository(owner: String, repo: String): Repository {
        return client
            .get("https://api.github.com/repos/$owner/$repo")
            .body()
    }

    fun getBranches(owner: String, repo: String, perPage: Int = 30): Flow<Branches.Branch> {
        return withPagination(perPage, "https://api.github.com/repos/$owner/$repo/branches", fetch = HttpResponse::body)
    }

    fun getCommits(owner: String, repo: String, sha: String = DEFAULT_BRANCH, perPage: Int = 30): Flow<Commits.Commit> {
        return withPagination(
            perPage,
            url = "https://api.github.com/repos/$owner/$repo/commits",
            customizer = {
                url {
                    if (sha !== DEFAULT_BRANCH) {
                        parameter("sha", sha)
                    }
                }
            },
            fetch = HttpResponse::body
        )
    }

    fun getPullRequests(owner: String, repo: String, baseBranch: String = DEFAULT_BRANCH, perPage: Int = 30): Flow<PullRequest> {
        return withPagination(
            perPage,
            url = "https://api.github.com/repos/$owner/$repo/pulls",
            customizer = {
                url {
                    if (baseBranch !== DEFAULT_BRANCH) {
                        parameter("base", baseBranch)
                    }
                }
            },
            fetch = HttpResponse::body
        )
    }

    suspend fun getPullRequest(owner: String, repo: String, pr: Int): PullRequest {
        return client
            .get("https://api.github.com/repos/$owner/$repo/pulls/$pr")
            .body()
    }

    suspend fun getLatestRelease(owner: String, repo: String): Release? {
        return client
            .get("https://api.github.com/repos/$owner/$repo/releases/latest")
            .also {
                if (it.status == HttpStatusCode.NotFound) {
                    return null
                }
            }
            .body()
    }

    fun getAllTags(owner: String, repo: String, perPage: Int = 30): Flow<Tag> {
        return withPagination(
            perPage,
            url = "https://api.github.com/repos/$owner/$repo/tags",
            fetch = HttpResponse::body
        )
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

    companion object {

        const val DEFAULT_BRANCH = "default_branch"
    }
}