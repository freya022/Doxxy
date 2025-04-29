package dev.freya02.doxxy.bot.config

import dev.freya02.doxxy.common.Env
import io.github.freya022.botcommands.api.core.service.annotations.BService

data class DatabaseConfig(
    val serverName: String,
    val port: Int,
    val name: String,
    val user: String,
    val password: String
) {
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
    val host: String,
    val port: Int,
    val examples: Examples
) {

    data class Examples(
        /** Enables linking examples under docs */
        val fromDocs: Boolean,
    )
}

data class Config(
    val dev: Boolean,
    val token: String,
    val ownerIds: List<Long>,
    val prefixes: List<String>,
    val testGuildIds: List<Long>,
    val fakeJDAGuildId: Long,
    val fakeBCGuildId: Long,
    val examplesHttpPort: Int,
    @get:BService
    val pullUpdater: PullUpdaterConfig,
    @get:BService
    val backend: BackendConfig,
    @get:BService
    val databaseConfig: DatabaseConfig
) {
    companion object {

        @get:BService
        val config: Config by lazy {
            Config(
                Env["DEV"].toBooleanStrict(),
                Env["BOT_TOKEN"],
                Env.getList("BOT_OWNER_IDS") { it.toLong() },
                Env.getList("BOT_PREFIXES"),
                Env.getList("BOT_TEST_GUILD_IDS") { it.toLong() },
                Env["BOT_FAKE_JDA_GUILD_ID"].toLong(),
                Env["BOT_FAKE_BC_GUILD_ID"].toLong(),
                Env["BOT_EXAMPLES_HTTP_PORT"].toInt(),
                PullUpdaterConfig(
                    Env["PULL_UPDATER_ENABLE"].toBooleanStrict(),
                    Env["PULL_UPDATER_GIT_TOKEN"],
                    Env["PULL_UPDATER_GIT_NAME"],
                    Env["PULL_UPDATER_GIT_EMAIL"],
                    Env["PULL_UPDATER_FORK_BOT_NAME"],
                    Env["PULL_UPDATER_FORK_REPO_NAME"],
                ),
                BackendConfig(
                    Env["BACKEND_ENABLE"].toBooleanStrict(),
                    Env["BACKEND_HOST"],
                    Env["BACKEND_PORT"].toInt(),
                    BackendConfig.Examples(
                        Env["BACKEND_EXAMPLES_FROM_DOCS"].toBooleanStrict(),
                    ),
                ),
                DatabaseConfig(
                    Env["DB_HOST"],
                    Env["DB_PORT"].toInt(),
                    Env["POSTGRES_DB"],
                    Env["POSTGRES_USER"],
                    Env["POSTGRES_PASSWORD"],
                )
            )
        }
    }
}