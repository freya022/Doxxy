package dev.freya02.doxxy.bot.config

import io.github.cdimascio.dotenv.Dotenv
import io.github.cdimascio.dotenv.dotenv
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
            val env = dotenv()

            Config(
                env["DEV"].toBooleanStrict(),
                env["BOT_TOKEN"],
                env.getList("BOT_OWNER_IDS") { it.toLong() },
                env.getList("BOT_PREFIXES"),
                env.getList("BOT_TEST_GUILD_IDS") { it.toLong() },
                env["BOT_FAKE_JDA_GUILD_ID"].toLong(),
                env["BOT_FAKE_BC_GUILD_ID"].toLong(),
                env["BOT_EXAMPLES_HTTP_PORT"].toInt(),
                PullUpdaterConfig(
                    env["PULL_UPDATER_ENABLE"].toBooleanStrict(),
                    env["PULL_UPDATER_GIT_TOKEN"],
                    env["PULL_UPDATER_GIT_NAME"],
                    env["PULL_UPDATER_GIT_EMAIL"],
                    env["PULL_UPDATER_FORK_BOT_NAME"],
                    env["PULL_UPDATER_FORK_REPO_NAME"],
                ),
                BackendConfig(
                    env["BACKEND_ENABLE"].toBooleanStrict(),
                    env["BACKEND_HOST"],
                    env["BACKEND_PORT"].toInt(),
                    BackendConfig.Examples(
                        env["BACKEND_EXAMPLES_FROM_DOCS"].toBooleanStrict(),
                    ),
                ),
                DatabaseConfig(
                    env["DB_HOST"],
                    env["DB_PORT"].toInt(),
                    env["POSTGRES_DB"],
                    env["POSTGRES_USER"],
                    env["POSTGRES_PASSWORD"],
                )
            )
        }

        private fun Dotenv.getList(name: String): List<String> = getList(name) { it }

        private fun <R> Dotenv.getList(name: String, transform: (String) -> R): List<R> {
            return get(name).split(",").map { it.trim().let(transform) }
        }
    }
}