package dev.freya02.doxxy.bot.providers

import dev.freya02.doxxy.github.client.GithubClient
import io.github.freya022.botcommands.api.core.service.annotations.BConfiguration
import io.github.freya022.botcommands.api.core.service.annotations.BService
import io.ktor.client.*

@BConfiguration
object GithubClientProvider {

    @BService
    fun githubClient(defaultClient: HttpClient): GithubClient = GithubClient(defaultClient)
}