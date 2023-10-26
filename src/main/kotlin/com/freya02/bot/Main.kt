package com.freya02.bot

import ch.qos.logback.classic.ClassicConstants
import com.freya02.botcommands.api.core.BBuilder
import com.freya02.botcommands.api.core.utils.namedDefaultScope
import com.freya02.docs.DocWebServer
import dev.minn.jda.ktx.events.CoroutineEventManager
import dev.reformator.stacktracedecoroutinator.runtime.DecoroutinatorRuntime
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.debug.DebugProbes
import mu.KotlinLogging
import net.dv8tion.jda.api.events.session.ShutdownEvent
import java.lang.management.ManagementFactory
import kotlin.io.path.absolutePathString
import kotlin.io.path.exists
import kotlin.system.exitProcess
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

object Main {
    private val logger by lazy { KotlinLogging.logger {} } // Must not load before system property is set

    @OptIn(ExperimentalCoroutinesApi::class)
    @JvmStatic
    fun main(args: Array<String>) {
        try {
            Data.init()

            if (Data.logbackConfigPath.exists()) {
                System.setProperty(ClassicConstants.CONFIG_FILE_PROPERTY, Data.logbackConfigPath.absolutePathString())
                logger.info( "Loading production logback config")
            } else {
                logger.info( "Loading test logback config")
            }

            //stacktrace-decoroutinator seems to have issues when reloading with hotswap agent
            if ("-XX:HotswapAgent=fatjar" in ManagementFactory.getRuntimeMXBean().inputArguments) {
                logger.info("Skipping stacktrace-decoroutinator as HotswapAgent is active")
            } else if ("--no-decoroutinator" in args) {
                logger.info("Skipping stacktrace-decoroutinator as --no-decoroutinator is specified")
            } else {
                DecoroutinatorRuntime.load()
            }

            DebugProbes.install()

            val scope = namedDefaultScope("Doxxy coroutine", 4)
            val manager = CoroutineEventManager(scope, 1.minutes)
            manager.listener<ShutdownEvent> {
                scope.cancel()
            }

            logger.info("Starting docs web server")
            DocWebServer.startDocWebServer()
            logger.info("Started docs web server")

            val config = Config.config
            BBuilder.newBuilder(manager) {
                if (Data.isDevEnvironment) {
                    disableExceptionsInDMs = true
                    disableAutocompleteCache = true
                }

                queryLogThreshold = 500.milliseconds

                addOwners(*config.ownerIds.toLongArray())

                addSearchPath("com.freya02.bot")

                textCommands {
                    usePingAsPrefix = true
                }

                applicationCommands {
                    testGuildIds += config.testGuildIds
                }

                components {
                    useComponents = true
                }
            }

            logger.info("Loaded commands")
        } catch (e: Exception) {
            logger.error("Unable to start the bot", e)
            exitProcess(-1)
        }
    }
}
