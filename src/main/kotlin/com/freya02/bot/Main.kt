package com.freya02.bot

import ch.qos.logback.classic.util.ContextInitializer
import com.freya02.botcommands.api.core.BBuilder
import com.freya02.docs.DocWebServer
import dev.minn.jda.ktx.events.CoroutineEventManager
import dev.minn.jda.ktx.events.getDefaultScope
import dev.reformator.stacktracedecoroutinator.runtime.DecoroutinatorRuntime
import kotlinx.coroutines.cancel
import mu.KotlinLogging
import net.dv8tion.jda.api.events.session.ShutdownEvent
import java.lang.management.ManagementFactory
import kotlin.io.path.absolutePathString
import kotlin.io.path.exists
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

            logger.info("Starting docs web server")
            DocWebServer.startDocWebServer()
            logger.info("Started docs web server")

            BBuilder.newBuilder({
                devMode = Data.testConfigPath.exists()

                addOwners(222046562543468545L)

                addSearchPath("com.freya02.bot")

                applicationCommands {
                    testGuildIds += 722891685755093072
                }

                components {
                    useComponents = true
                }

                settingsProvider = BotSettings
            }, manager)

            logger.info("Loaded commands")
        } catch (e: Exception) {
            logger.error("Unable to start the bot", e)
            exitProcess(-1)
        }
    }
}