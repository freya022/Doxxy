package com.freya02.bot

import ch.qos.logback.classic.util.ContextInitializer
import com.freya02.bot.db.Database
import com.freya02.bot.docs.DocIndexMap
import com.freya02.bot.versioning.Versions
import com.freya02.botcommands.api.core.BBuilder
import com.freya02.docs.DocWebServer
import dev.minn.jda.ktx.events.CoroutineEventManager
import dev.minn.jda.ktx.events.getDefaultScope
import dev.minn.jda.ktx.jdabuilder.light
import dev.reformator.stacktracedecoroutinator.runtime.DecoroutinatorRuntime
import kotlinx.coroutines.cancel
import mu.KotlinLogging
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.events.session.ShutdownEvent
import net.dv8tion.jda.api.requests.GatewayIntent
import net.dv8tion.jda.api.utils.cache.CacheFlag
import java.util.function.Supplier
import kotlin.io.path.absolutePathString
import kotlin.system.exitProcess
import kotlin.time.Duration.Companion.minutes

object Main {
    private val logger by lazy { KotlinLogging.logger {} } // Must not load before system property is set

    @JvmStatic
    fun main(args: Array<String>) {
        try {
            DecoroutinatorRuntime.load()

            Data.init()

            System.setProperty(ContextInitializer.CONFIG_FILE_PROPERTY, Data.logbackConfigPath.absolutePathString())

            val scope = getDefaultScope()
            val manager = CoroutineEventManager(scope, 1.minutes)
            manager.listener<ShutdownEvent> {
                scope.cancel()
            }

            val config = Config.config

            val database = Database(config)

            logger.info("Starting docs web server")
            DocWebServer.startDocWebServer()
            logger.info("Started docs web server")

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
                    useComponents = true
                }

                settingsProvider = BotSettings
            }, manager)

            logger.info("Loaded commands")

            light(config.token, enableCoroutines = false) {
                enableCache(CacheFlag.CLIENT_STATUS)
                enableIntents(GatewayIntent.GUILD_PRESENCES, GatewayIntent.MESSAGE_CONTENT)

                setMaxReconnectDelay(128)
                setActivity(Activity.watching("the docs"))
                setEventManager(manager)
            }.awaitReady()

            logger.info("Loaded JDA")
        } catch (e: Exception) {
            logger.error("Unable to start the bot", e)
            exitProcess(-1)
        }
    }
}