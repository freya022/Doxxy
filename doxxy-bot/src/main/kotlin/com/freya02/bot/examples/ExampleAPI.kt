package com.freya02.bot.examples

import com.freya02.bot.switches.RequiresBackend
import io.github.freya022.botcommands.api.core.service.annotations.BService
import io.github.freya022.doxxy.common.dto.ExampleDTO
import io.github.freya022.doxxy.common.dto.ExampleSearchResultDTO
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import java.io.IOException

@RequiresBackend
@BService
class ExampleAPI(
    private val backendClient: HttpClient
) {
    suspend fun updateExamples() {
        backendClient.put("examples/update")
    }

    suspend fun searchExamplesByTarget(target: String): List<ExampleSearchResultDTO> {
        return backendClient
            .get("examples") {
                url {
                    parameter("target", target)
                }
            }
            .body()
    }

    suspend fun searchExamplesByTitle(title: String): List<ExampleSearchResultDTO> {
        return backendClient
            .get("examples/search") {
                url {
                    parameter("query", title)
                }
            }
            .body()
    }

    suspend fun getExampleByTitle(title: String): ExampleDTO? {
        val response = backendClient.get("example") {
            url {
                parameter("title", title)
            }
        }
        return when (response.status) {
            HttpStatusCode.OK -> response.body()
            HttpStatusCode.NotFound -> null
            //TODO replace with error message from API later
            else -> throw IOException("Status code: ${response.status}")
        }
    }

    suspend fun getLanguagesByTitle(title: String): List<String> {
        return backendClient.get("example/languages") {
            url {
                parameter("title", title)
            }
        }.body()
    }
}