package com.freya02.bot

import ch.qos.logback.classic.util.ContextInitializer
import com.freya02.bot.db.DatabaseSource
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
import java.lang.management.ManagementFactory
import java.util.function.Supplier
import kotlin.io.path.absolutePathString
import kotlin.system.exitProcess
import kotlin.time.Duration.Companion.minutes

object Main {
    private val logger by lazy { KotlinLogging.logger {} } // Must not load before system property is set

    @JvmStatic
    fun main(args: Array<String>) {
        try {
            Data.init()

            System.setProperty(ContextInitializer.CONFIG_FILE_PROPERTY, Data.logbackConfigPath.absolutePathString())

            //stacktrace-decoroutinator seems to have issues when reloading with hotswap agent
            if ("-XX:HotswapAgent=fatjar" !in ManagementFactory.getRuntimeMXBean().inputArguments) {
                DecoroutinatorRuntime.load()
            } else {
                logger.info("Skipping stacktrace-decoroutinator as HotswapAgent is active")
            }

            val scope = getDefaultScope()
            val manager = CoroutineEventManager(scope, 1.minutes)
            manager.listener<ShutdownEvent> {
                scope.cancel()
            }

            //TODO v3: This should be avoided, the framework should be able to construct a config (via a function that returns the config)
            val config = Config.config

            val database = DatabaseSource(config)

            logger.info("Starting docs web server")
            DocWebServer.startDocWebServer()
            logger.info("Started docs web server")

            BBuilder.newBuilder({
                addOwners(222046562543468545L)

                addSearchPath("com.freya02.bot")

                serviceConfig.apply {
                    registerInstanceSupplier(Config::class.java) { config }
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