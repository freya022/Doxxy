package dev.freya02.doxxy.bot

import ch.qos.logback.classic.ClassicConstants
import dev.freya02.doxxy.bot.config.Config
import dev.reformator.stacktracedecoroutinator.jvm.DecoroutinatorJvmApi
import io.github.freya022.botcommands.api.core.BotCommands
import io.github.freya022.botcommands.api.core.config.DevConfig
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.debug.DebugProbes
import java.lang.management.ManagementFactory
import kotlin.io.path.*
import kotlin.system.exitProcess
import kotlin.time.Duration.Companion.milliseconds

object Main {
    private val logger by lazy { KotlinLogging.logger {} } // Must not load before system property is set

    @OptIn(ExperimentalCoroutinesApi::class)
    @JvmStatic
    fun main(args: Array<String>) {
        try {
            val logbackPath = Path("config", "logback.xml")
            if (logbackPath.exists()) {
                System.setProperty(ClassicConstants.CONFIG_FILE_PROPERTY, logbackPath.absolutePathString())
                logger.info { "Loading logback configuration from current directory (${logbackPath.absolute().parent.pathString})" }
            } else {
                logger.info { "Using packaged logback configuration as there is no logback.xml in ${logbackPath.absolute().parent.pathString}" }
            }

            //stacktrace-decoroutinator seems to have issues when reloading with hotswap agent
            if ("-XX:HotswapAgent=fatjar" in ManagementFactory.getRuntimeMXBean().inputArguments) {
                logger.info { "Skipping stacktrace-decoroutinator as HotswapAgent is active" }
            } else if ("--no-decoroutinator" in args) {
                logger.info { "Skipping stacktrace-decoroutinator as --no-decoroutinator is specified" }
            } else {
                DecoroutinatorJvmApi.install()
            }

            DebugProbes.install()

            val config = Config.config
            BotCommands.create {
                disableExceptionsInDMs = config.dev

                database {
                    queryLogThreshold = 500.milliseconds
                }

                addPredefinedOwners(*config.ownerIds.toLongArray())

                addSearchPath("dev.freya02.doxxy.bot")

                textCommands {
                    usePingAsPrefix = true
                }

                applicationCommands {
                    @OptIn(DevConfig::class)
                    disableAutocompleteCache = config.dev

                    testGuildIds += config.testGuildIds

                    databaseCache {

                    }
                }

                components {
                    enable = true
                }

                appEmojis {
                    enable = true
                }
            }

            logger.info { "Loaded commands" }
        } catch (e: Exception) {
            logger.error(e) { "Unable to start the bot" }
            exitProcess(-1)
        }
    }
}
