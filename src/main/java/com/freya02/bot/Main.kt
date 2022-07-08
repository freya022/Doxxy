package com.freya02.bot

import com.freya02.bot.db.Database
import com.freya02.bot.docs.DocSourceTypeResolver
import com.freya02.bot.tag.TagCriteriaResolver
import com.freya02.bot.versioning.LibraryTypeResolver
import com.freya02.bot.versioning.Versions
import com.freya02.botcommands.api.CommandsBuilder
import com.freya02.botcommands.api.Logging
import com.freya02.botcommands.api.builder.ApplicationCommandsBuilder
import com.freya02.botcommands.api.builder.ExtensionsBuilder
import com.freya02.botcommands.api.builder.TextCommandsBuilder
import com.freya02.botcommands.api.components.DefaultComponentManager
import com.freya02.botcommands.api.runner.KotlinMethodRunnerFactory
import com.freya02.docs.DocWebServer
import dev.minn.jda.ktx.events.CoroutineEventManager
import dev.minn.jda.ktx.events.getDefaultScope
import dev.minn.jda.ktx.jdabuilder.light
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.events.ShutdownEvent
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.system.exitProcess
import kotlin.time.Duration.Companion.minutes

private val LOGGER = Logging.getLogger()

object Main {
    @JvmField
    val BOT_FOLDER: Path = Path.of(System.getProperty("user.home"), "Downloads", "DocsBot")
    @JvmField
    val CACHE_PATH: Path = BOT_FOLDER.resolve("docs_cache")
    @JvmField
    val JAVADOCS_PATH: Path = BOT_FOLDER.resolve("javadocs")
    @JvmField
    val REPOS_PATH: Path = BOT_FOLDER.resolve("repos")
    @JvmField
    val LAST_KNOWN_VERSIONS_FOLDER_PATH: Path = BOT_FOLDER.resolve("last_versions")
    @JvmField
    val BRANCH_VERSIONS_FOLDER_PATH: Path = LAST_KNOWN_VERSIONS_FOLDER_PATH.resolve("branch_versions")

    init {
        check(BOT_FOLDER.exists()) { "Bot folder at $BOT_FOLDER does not exist !" }

        Files.createDirectories(CACHE_PATH)
        Files.createDirectories(REPOS_PATH)
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

            val jda = light(config.token, enableCoroutines = false) {
                setActivity(Activity.watching("the docs"))
                setEventManager(manager)
            }.awaitReady()

            LOGGER.info("Loaded JDA")

            val database = Database(config)

            LOGGER.info("Starting docs web server")
            DocWebServer.startDocWebServer()
            LOGGER.info("Started docs web server")

            val versions = Versions()
            val commandsBuilder = CommandsBuilder.newBuilder(222046562543468545L)

            commandsBuilder
                .extensionsBuilder { extensionsBuilder: ExtensionsBuilder ->
                    extensionsBuilder
                        .registerConstructorParameter(Config::class.java) { config }
                        .registerConstructorParameter(Database::class.java) { database }
                        .registerParameterResolver(TagCriteriaResolver())
                        .registerParameterResolver(DocSourceTypeResolver())
                        .registerConstructorParameter(Versions::class.java) { versions }
                        .registerParameterResolver(LibraryTypeResolver())
                        .setMethodRunnerFactory(KotlinMethodRunnerFactory(Dispatchers.IO, scope))
                }
                .textCommandBuilder { textCommandsBuilder: TextCommandsBuilder ->
                    textCommandsBuilder
                        .addPrefix("!")
                }
                .applicationCommandBuilder { applicationCommandsBuilder: ApplicationCommandsBuilder ->
                    applicationCommandsBuilder
                        .addTestGuilds(722891685755093072L)
                }
                .setComponentManager(DefaultComponentManager { database.connection })
                .setSettingsProvider(BotSettings())
                .build(jda, "com.freya02.bot.commands")

            versions.initUpdateLoop(commandsBuilder.context)

            LOGGER.info("Loaded commands")
        } catch (e: Exception) {
            LOGGER.error("Unable to start the bot", e)
            exitProcess(-1)
        }
    }
}