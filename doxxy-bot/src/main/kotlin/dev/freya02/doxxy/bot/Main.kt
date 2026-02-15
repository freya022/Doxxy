package dev.freya02.doxxy.bot

import ch.qos.logback.classic.ClassicConstants
import dev.freya02.botcommands.method.accessors.api.annotations.ExperimentalMethodAccessorsApi
import dev.freya02.doxxy.bot.config.Config
import io.github.freya022.botcommands.api.core.BotCommands
import io.github.freya022.botcommands.api.core.config.DevConfig
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.debug.DebugProbes
import kotlin.io.path.*
import kotlin.system.exitProcess
import kotlin.time.Duration.Companion.milliseconds

object Main {
    private val logger by lazy { KotlinLogging.logger {} } // Must not load before system property is set

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

            @OptIn(ExperimentalCoroutinesApi::class)
            DebugProbes.install()

            @OptIn(ExperimentalMethodAccessorsApi::class)
            BotCommands.preferClassFileAccessors()

            val config = Config.config
            BotCommands.create {
                disableExceptionsInDMs = config.dev

                database {
                    queryLogThreshold = 500.milliseconds
                }

                addPredefinedOwners(config.ownerIds)

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

            logger.info { "Finished loading" }
        } catch (e: Exception) {
            logger.error(e) { "Unable to start the bot" }
            exitProcess(-1)
        }
    }
}
