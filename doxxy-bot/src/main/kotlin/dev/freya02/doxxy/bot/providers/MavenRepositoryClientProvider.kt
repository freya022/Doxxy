package dev.freya02.doxxy.bot.providers

import dev.freya02.doxxy.bot.versioning.maven.MavenRepositoryClient
import io.github.freya022.botcommands.api.core.service.annotations.BConfiguration
import io.github.freya022.botcommands.api.core.service.annotations.BService

@BConfiguration
object MavenRepositoryClientProvider {

    @BService
    fun mavenCentralClient(): MavenRepositoryClient =
        MavenRepositoryClient("https://repo.maven.apache.org/maven2")
}