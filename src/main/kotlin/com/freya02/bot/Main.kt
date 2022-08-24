package com.freya02.bot

import com.freya02.bot.db.Database
import com.freya02.bot.docs.DocIndexMap
import com.freya02.bot.versioning.Versions
import com.freya02.botcommands.api.Logging
import com.freya02.botcommands.api.components.DefaultComponentManager
import com.freya02.botcommands.api.core.BBuilder
import com.freya02.docs.DocWebServer
import dev.minn.jda.ktx.events.CoroutineEventManager
import dev.minn.jda.ktx.events.getDefaultScope
import dev.minn.jda.ktx.jdabuilder.light
import kotlinx.coroutines.cancel
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.events.ShutdownEvent
import net.dv8tion.jda.api.requests.GatewayIntent
import net.dv8tion.jda.api.utils.cache.CacheFlag
import java.nio.file.Path
import java.util.function.Supplier
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.system.exitProcess
import kotlin.time.Duration.Companion.minutes

private val LOGGER = Logging.getLogger()

object Main {
    @JvmField
    val BOT_FOLDER: Path = Path.of(System.getProperty("user.home"), "Downloads", "DocsBot")
    @JvmField
    val JAVADOCS_PATH: Path = BOT_FOLDER.resolve("javadocs")
    val LAST_KNOWN_VERSIONS_FOLDER_PATH: Path = BOT_FOLDER.resolve("last_versions")
    val BRANCH_VERSIONS_FOLDER_PATH: Path = LAST_KNOWN_VERSIONS_FOLDER_PATH.resolve("branch_versions")
    val PAGE_CACHE_FOLDER_PATH: Path = BOT_FOLDER.resolve("page_cache")

    init {
        check(BOT_FOLDER.exists()) { "Bot folder at $BOT_FOLDER does not exist !" }

        LAST_KNOWN_VERSIONS_FOLDER_PATH.createDirectories()
        BRANCH_VERSIONS_FOLDER_PATH.createDirectories()
        PAGE_CACHE_FOLDER_PATH.createDirectories()
    }

    @JvmStatic
    fun main(args: Array<String>) {
        try {
            val scope = getDefaultScope()
            val manager = CoroutineEventManager(scope, 1.minutes)
            manager.listener<ShutdownEvent> {
                scope.cancel()
            }

            val config = Config.getConfig()

            val database = Database(config)

            LOGGER.info("Starting docs web server")
            DocWebServer.startDocWebServer()
            LOGGER.info("Started docs web server")

            val docIndexMap = DocIndexMap(database)

            val versions = Versions(docIndexMap)

            BBuilder.newBuilder({
                addOwners(222046562543468545L)

                addSearchPath("com.freya02.bot")

                serviceConfig.apply {
                    registerInstanceSupplier(Config::class.java) { config }
                    registerInstanceSupplier(Database::class.java) { database }
                    registerInstanceSupplier(Versions::class.java) { versions }
                    registerInstanceSupplier(DocIndexMap::class.java) { docIndexMap }
                }

                connectionProvider = Supplier { database.fetchConnection() }

                applicationCommands {
                    testGuildIds += 722891685755093072
                }

                components {
                    componentManagerStrategy = DefaultComponentManager::class.java
                }

                settingsProvider = BotSettings
            }, manager)

            LOGGER.info("Loaded commands")

            light(config.token, enableCoroutines = false) {
                enableCache(CacheFlag.CLIENT_STATUS)
                enableIntents(GatewayIntent.GUILD_PRESENCES)

                setActivity(Activity.watching("the docs"))
                setEventManager(manager)
            }.awaitReady()

            LOGGER.info("Loaded JDA")
        } catch (e: Exception) {
            LOGGER.error("Unable to start the bot", e)
            exitProcess(-1)
        }
    }
}