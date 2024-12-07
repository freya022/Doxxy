package com.freya02.bot.config

import com.google.gson.Gson
import io.github.freya022.botcommands.api.core.service.annotations.BService
import io.github.oshai.kotlinlogging.KotlinLogging
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.readText

data class DatabaseConfig(val serverName: String, val port: Int, val name: String, val user: String, val password: String) {
    val url: String
        get() = "jdbc:postgresql://$serverName:$port/$name"
}

data class PullUpdaterConfig(
    val enable: Boolean,
    val gitToken: String, val gitName: String, val gitEmail: String,
    val forkBotName: String, val forkRepoName: String
)

data class BackendConfig(
    val enable: Boolean,
    val url: String,
    val examples: Examples
) {

    data class Examples(
        /** Enables linking examples under docs */
        val fromDocs: Boolean,
    )
}

data class Config(val token: String,
                  val ownerIds: List<Long>,
                  val prefixes: List<String>,
                  val testGuildIds: List<Long>,
                  val fakeJDAGuildId: Long,
                  val fakeBCGuildId: Long,
                  val pullUpdater: PullUpdaterConfig,
                  val backend: BackendConfig,
                  val databaseConfig: DatabaseConfig
) {
    companion object {
        private val logger = KotlinLogging.logger { }

        private val configFilePath: Path = Environment.configFolder.resolve("config.json")

        @get:BService
        val config: Config by lazy {
            logger.info { "Loading configuration at ${configFilePath.absolutePathString()}" }

            return@lazy Gson().fromJson(configFilePath.readText(), Config::class.java)
        }

        @get:BService
        val databaseConfig: DatabaseConfig get() = config.databaseConfig

        @get:BService
        val pullUpdateConfig: PullUpdaterConfig get() = config.pullUpdater

        @get:BService
        val backendConfig: BackendConfig get() = config.backend
    }
}