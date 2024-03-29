package com.freya02.bot.providers

import com.freya02.bot.config.BackendConfig
import com.freya02.bot.switches.RequiresBackend
import io.github.freya022.botcommands.api.core.service.annotations.BConfiguration
import io.github.freya022.botcommands.api.core.service.annotations.BService
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*

@BConfiguration
object HttpClientProvider {
    //TODO use in PullUpdater after removing gson
    @BService
    fun defaultClient(): HttpClient = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json()
        }
    }

    @RequiresBackend
    @BService
    fun backendClient(
        defaultClient: HttpClient,
        backendConfig: BackendConfig
    ): HttpClient = defaultClient.config {
        defaultRequest {
            url(backendConfig.url)
        }
    }
}