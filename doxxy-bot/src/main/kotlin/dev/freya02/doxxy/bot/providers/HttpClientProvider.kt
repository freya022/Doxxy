package dev.freya02.doxxy.bot.providers

import io.github.freya022.botcommands.api.core.service.annotations.BConfiguration
import io.github.freya022.botcommands.api.core.service.annotations.BService
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*

@BConfiguration
object HttpClientProvider {
    //TODO use in PullUpdater after removing gson
    @BService
    fun defaultClient(): HttpClient = HttpClient(OkHttp) {

    }
}