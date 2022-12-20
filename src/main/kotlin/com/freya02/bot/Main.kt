package com.freya02.bot

import ch.qos.logback.classic.util.ContextInitializer
import com.freya02.bot.db.Database
import com.freya02.bot.docs.DocIndexMap
import com.freya02.bot.docs.DocSourceTypeResolver
import com.freya02.bot.tag.TagCriteriaResolver
import com.freya02.bot.utils.LogLevelResolver
import com.freya02.bot.versioning.LibraryTypeResolver
import com.freya02.bot.versioning.Versions
import com.freya02.botcommands.api.CommandsBuilder
import com.freya02.botcommands.api.builder.ApplicationCommandsBuilder
import com.freya02.botcommands.api.builder.ExtensionsBuilder
import com.freya02.botcommands.api.components.DefaultComponentManager
import com.freya02.botcommands.api.runner.KotlinMethodRunnerFactory
import com.freya02.docs.DocWebServer
import dev.minn.jda.ktx.events.CoroutineEventManager
import dev.minn.jda.ktx.events.getDefaultScope
import dev.minn.jda.ktx.jdabuilder.light
import dev.reformator.stacktracedecoroutinator.runtime.DecoroutinatorRuntime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.events.session.ShutdownEvent
import net.dv8tion.jda.api.requests.GatewayIntent
import net.dv8tion.jda.api.utils.cache.CacheFlag
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

            val jda = light(config.token, enableCoroutines = false) {
                enableCache(CacheFlag.CLIENT_STATUS)
                enableIntents(GatewayIntent.GUILD_PRESENCES, GatewayIntent.MESSAGE_CONTENT)

                setMaxReconnectDelay(128)
                setActivity(Activity.watching("the docs"))
                setEventManager(manager)
            }.awaitReady()

            logger.info("Loaded JDA")

            val database = Database(config)

            logger.info("Starting docs web server")
            DocWebServer.startDocWebServer()
            logger.info("Started docs web server")

            val docIndexMap = DocIndexMap(database)

            val versions = Versions(docIndexMap)
            val commandsBuilder = CommandsBuilder.newBuilder(222046562543468545L)

            commandsBuilder
                .textCommandBuilder { it.addPrefix(";") }
                .extensionsBuilder { extensionsBuilder: ExtensionsBuilder ->
                    extensionsBuilder
                        .registerConstructorParameter(Config::class.java) { config }
                        .registerConstructorParameter(Database::class.java) { database }
                        .registerParameterResolver(TagCriteriaResolver())
                        .registerParameterResolver(DocSourceTypeResolver())
                        .registerConstructorParameter(Versions::class.java) { versions }
                        .registerConstructorParameter(DocIndexMap::class.java) { docIndexMap }
                        .registerParameterResolver(LibraryTypeResolver())
                        .registerParameterResolver(LogLevelResolver())
                        .setMethodRunnerFactory(KotlinMethodRunnerFactory(Dispatchers.IO, scope))
                }
                .applicationCommandBuilder { applicationCommandsBuilder: ApplicationCommandsBuilder ->
                    applicationCommandsBuilder
                        .addTestGuilds(722891685755093072L)
                }
                .setComponentManager(DefaultComponentManager { database.fetchConnection() })
                .setSettingsProvider(BotSettings())
                .build(jda, "com.freya02.bot.commands")

            runBlocking {
                versions.initUpdateLoop(commandsBuilder.context)
            }

            logger.info("Loaded commands")
        } catch (e: Exception) {
            logger.error("Unable to start the bot", e)
            exitProcess(-1)
        }
    }
}