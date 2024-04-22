package com.freya02.bot.versioning

import com.freya02.bot.commands.slash.docs.CommonDocsHandlers
import com.freya02.bot.config.Data
import com.freya02.bot.docs.DocIndexMap
import com.freya02.bot.docs.index.ReindexData
import com.freya02.bot.utils.Utils.withTemporaryFile
import com.freya02.bot.versioning.VersionsUtils.downloadMavenJavadoc
import com.freya02.bot.versioning.VersionsUtils.downloadMavenSources
import com.freya02.bot.versioning.github.GithubUtils
import com.freya02.bot.versioning.maven.DependencyVersionChecker
import com.freya02.bot.versioning.maven.MavenVersionChecker
import com.freya02.bot.versioning.maven.RepoType
import com.freya02.docs.DocSourceType
import dev.minn.jda.ktx.events.getDefaultScope
import io.github.freya022.botcommands.api.core.BContext
import io.github.freya022.botcommands.api.core.annotations.BEventListener
import io.github.freya022.botcommands.api.core.events.InjectedJDAEvent
import io.github.freya022.botcommands.api.core.service.annotations.BService
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.*
import java.util.concurrent.Executors
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

private val logger = KotlinLogging.logger { }

@BService
class Versions(
    private val context: BContext,
    private val docIndexMap: DocIndexMap,
    private val versionsRepository: VersionsRepository,
) {
    private val bcChecker =
        MavenVersionChecker(versionsRepository.getInitialVersion(LibraryType.BOT_COMMANDS), RepoType.MAVEN)
    val latestBotCommandsVersion: ArtifactInfo
        get() = bcChecker.latest

    private val bcJdaVersionChecker =
        DependencyVersionChecker(
            versionsRepository.getInitialVersion(LibraryType.JDA, "freya022-botcommands-release"),
            targetArtifactId = "JDA"
        ) { bcChecker.latest.toMavenUrl(FileType.POM) }
    val jdaVersionFromBotCommands: ArtifactInfo
        get() = bcJdaVersionChecker.latest

    private val jdaChecker: MavenVersionChecker =
        MavenVersionChecker(versionsRepository.getInitialVersion(LibraryType.JDA), RepoType.MAVEN)
    val latestJDAVersion: ArtifactInfo
        get() = jdaChecker.latest

    private val jdaKtxChecker: MavenVersionChecker =
        MavenVersionChecker(versionsRepository.getInitialVersion(LibraryType.JDA_KTX), RepoType.MAVEN)
    val latestJDAKtxVersion: ArtifactInfo
        get() = jdaKtxChecker.latest

    private val lavaPlayerChecker: MavenVersionChecker =
        MavenVersionChecker(versionsRepository.getInitialVersion(LibraryType.LAVA_PLAYER), RepoType.MAVEN)
    val latestLavaPlayerVersion: ArtifactInfo
        get() = lavaPlayerChecker.latest

    @BEventListener(async = true, timeout = -1)
    suspend fun initUpdateLoop(event: InjectedJDAEvent) {
        val scope = getDefaultScope(
            pool = Executors.newSingleThreadScheduledExecutor { Thread(it, "Version check coroutine") },
            context = CoroutineName("Version check coroutine")
        )

        scope.scheduleWithFixedDelay(30.minutes, "BC", ::checkLatestBCVersion)
        scope.scheduleWithFixedDelay(30.minutes, "JDA", ::checkLatestJDAVersion)
        scope.scheduleWithFixedDelay(30.minutes, "JDA-KTX", ::checkLatestJDAKtxVersion)
        scope.scheduleWithFixedDelay(30.minutes, "LavaPlayer", ::checkLatestLavaPlayerVersion)

        //First index for Java's docs, may take some time
        if (docIndexMap[DocSourceType.JAVA]!!.getClassDoc("Object") == null) {
            docIndexMap[DocSourceType.JAVA]!!.reindex(ReindexData())

            //Once java's docs are indexed, invalidate caches if the user had time to use the commands before docs were loaded
            for (autocompleteName in CommonDocsHandlers.AUTOCOMPLETE_NAMES) {
                context.invalidateAutocompleteCache(autocompleteName)
            }
        }
    }

    private inline fun CoroutineScope.scheduleWithFixedDelay(duration: Duration, desc: String, crossinline block: suspend () -> Unit) {
        launch {
            try {
                while (true) {
                    try {
                        withTimeout(10.minutes) {
                            block()
                        }
                    } catch (e: Throwable) {
                        logger.error(e) { "An error occurred while checking latest $desc version" }
                    }
                    delay(duration)
                }
            } finally {
                throw IllegalStateException("Exited version check loop")
            }
        }
    }

    //TODO use coroutines in version checkers / HTTP
    private suspend fun checkLatestJDAVersion() {
        val changed = jdaChecker.checkVersion()

        if (changed) {
            val sourceUrl = GithubUtils.getLatestReleaseHash("discord-jda", "JDA")
                ?.let { hash -> "https://github.com/discord-jda/JDA/blob/${hash.hash}/src/main/java/" }

            if (sourceUrl == versionsRepository.findByName(LibraryType.JDA, classifier = null)?.sourceUrl)
                return logger.debug { "Ignoring new JDA version (${jdaChecker.latest}) from Maven Central as the GitHub release hasn't been made" }

            logger.info { "JDA version changed" }

            logger.trace { "Downloading JDA javadocs" }
            val jdaDocsFolder = Data.jdaDocsFolder
            jdaChecker.latest.downloadMavenJavadoc().withTemporaryFile { tempZip ->
                logger.trace { "Extracting JDA javadocs" }
                VersionsUtils.replaceWithZipContent(tempZip, jdaDocsFolder, "html")
            }

            jdaChecker.latest.downloadMavenSources().withTemporaryFile { tempZip ->
                logger.trace { "Extracting JDA sources" }
                VersionsUtils.extractZip(tempZip, jdaDocsFolder, "java")
            }

            logger.trace { "Invalidating JDA index" }
            docIndexMap.refreshAndInvalidateIndex(DocSourceType.JDA, ReindexData(sourceUrl))
            for (handlerName in CommonDocsHandlers.AUTOCOMPLETE_NAMES) {
                context.invalidateAutocompleteCache(handlerName)
            }

            jdaChecker.saveVersion(sourceUrl = sourceUrl)

            logger.info { "JDA version updated to ${jdaChecker.latest.version}" }
        }
    }

    private suspend fun checkLatestJDAKtxVersion() {
        val changed = jdaKtxChecker.checkVersion()
        if (changed) {
            logger.info { "JDA-KTX version changed" }
            jdaKtxChecker.saveVersion()
            logger.info { "JDA-KTX version updated to ${jdaKtxChecker.latest.version}" }
        }
    }

    private suspend fun checkLatestLavaPlayerVersion() {
        val changed = lavaPlayerChecker.checkVersion()
        if (changed) {
            logger.info { "LavaPlayer version changed" }
            lavaPlayerChecker.saveVersion()
            logger.info { "LavaPlayer version updated to ${lavaPlayerChecker.latest.version}" }
        }
    }

    private suspend fun checkLatestBCVersion() {
        val changed = bcChecker.checkVersion()

        if (changed) {
            logger.info { "BotCommands version changed" }

            bcChecker.saveVersion()
            bcJdaVersionChecker.checkVersion()
            bcJdaVersionChecker.saveVersion()

            logger.info { "BotCommands version updated to ${bcChecker.latest.version}" }
        }
    }

    private suspend fun VersionChecker.saveVersion(sourceUrl: String? = null) {
        versionsRepository.save(LibraryVersion(classifier, latest, sourceUrl))
    }
}
