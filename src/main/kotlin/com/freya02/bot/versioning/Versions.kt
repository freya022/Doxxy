package com.freya02.bot.versioning

import com.freya02.bot.Data
import com.freya02.bot.commands.slash.docs.CommonDocsHandlers
import com.freya02.bot.docs.DocIndexMap
import com.freya02.bot.docs.index.ReindexData
import com.freya02.bot.utils.Utils.withTemporaryFile
import com.freya02.bot.versioning.VersionsUtils.downloadMavenJavadoc
import com.freya02.bot.versioning.VersionsUtils.downloadMavenSources
import com.freya02.bot.versioning.github.GithubUtils
import com.freya02.bot.versioning.jitpack.JitpackVersionChecker
import com.freya02.bot.versioning.maven.MavenBranchProjectDependencyVersionChecker
import com.freya02.bot.versioning.maven.MavenVersionChecker
import com.freya02.bot.versioning.maven.RepoType
import com.freya02.botcommands.api.BContext
import com.freya02.botcommands.api.core.annotations.BEventListener
import com.freya02.botcommands.api.core.annotations.BService
import com.freya02.botcommands.api.core.events.FirstReadyEvent
import com.freya02.docs.DocSourceType
import dev.minn.jda.ktx.events.getDefaultScope
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mu.KotlinLogging
import java.util.concurrent.Executors
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

@BService
class Versions(private val docIndexMap: DocIndexMap) {
    private val lastKnownBotCommandsPath = Data.getVersionPath(VersionType.BotCommands)
    private val lastKnownJDAFromBCPath = Data.getVersionPath(VersionType.JDAOfBotCommands)
    private val lastKnownJDAPath = Data.getVersionPath(VersionType.JDA)
    private val lastKnownJDAKtxPath = Data.getVersionPath(VersionType.JDAKTX)
    private val lastKnownLavaPlayerPath = Data.getVersionPath(VersionType.LAVAPLAYER)
    private val jdaDocsFolder = Data.jdaDocsFolder
    private val bcDocsFolder = Data.bcDocsFolder

    private val bcChecker =
        MavenVersionChecker(lastKnownBotCommandsPath, RepoType.MAVEN, "io.github.freya022", "BotCommands")
    private val jdaVersionFromBCChecker: MavenBranchProjectDependencyVersionChecker =
        MavenBranchProjectDependencyVersionChecker(lastKnownJDAFromBCPath, "freya022", "BotCommands", "JDA", "master")
    private val jdaChecker: MavenVersionChecker =
        MavenVersionChecker(lastKnownJDAPath, RepoType.MAVEN, "net.dv8tion", "JDA")
    private val jdaKtxChecker: JitpackVersionChecker =
        JitpackVersionChecker(lastKnownJDAKtxPath, "MinnDevelopment", "com.github.MinnDevelopment", "jda-ktx")
    private val lavaPlayerChecker: JitpackVersionChecker =
        JitpackVersionChecker(lastKnownLavaPlayerPath, "Walkyst", "com.github.Walkyst", "lavaplayer-fork")

    @BEventListener(async = true, timeout = -1)
    suspend fun initUpdateLoop(event: FirstReadyEvent, context: BContext) {
        val scope = getDefaultScope(
            pool = Executors.newSingleThreadScheduledExecutor { Thread(it, "Version check coroutine") },
            context = CoroutineName("Version check coroutine")
        )

        scope.scheduleWithFixedDelay(30.minutes, "BC") { checkLatestBCVersion(context) }
        scope.scheduleWithFixedDelay(30.minutes, "JDA from BC") { checkLatestJDAVersionFromBC() }
        scope.scheduleWithFixedDelay(30.minutes, "JDA") { checkLatestJDAVersion(context) }
        scope.scheduleWithFixedDelay(30.minutes, "JDA-KTX") { checkLatestJDAKtxVersion() }
        scope.scheduleWithFixedDelay(30.minutes, "LavaPlayer") { checkLatestLavaPlayerVersion() }

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
                block()
            } catch (e: Throwable) {
                logger.error("An error occurred while checking latest $desc version", e)
            }
            delay(duration)
        }
    }

    private suspend fun checkLatestJDAVersion(context: BContext) {
        val changed = jdaChecker.checkVersion()

        if (changed) {
            logger.info("JDA version changed")

            logger.trace("Downloading JDA javadocs")
            jdaChecker.latest.downloadMavenJavadoc().withTemporaryFile { tempZip ->
                logger.trace("Extracting JDA javadocs")
                VersionsUtils.replaceWithZipContent(tempZip, jdaDocsFolder, "html")
            }

            jdaChecker.latest.downloadMavenSources().withTemporaryFile { tempZip ->
                logger.trace("Extracting JDA sources")
                VersionsUtils.extractZip(tempZip, jdaDocsFolder, "java")
            }

            val sourceUrl = GithubUtils.getLatestReleaseHash("DV8FromTheWorld", "JDA")
                ?.let { hash -> "https://github.com/DV8FromTheWorld/JDA/blob/${hash.hash}/src/main/java/" }

            logger.trace("Invalidating JDA index")
            docIndexMap.refreshAndInvalidateIndex(DocSourceType.JDA, ReindexData(sourceUrl))
            for (handlerName in CommonDocsHandlers.AUTOCOMPLETE_NAMES) {
                context.invalidateAutocompleteCache(handlerName)
            }

            jdaChecker.saveVersion()

            logger.info("JDA version updated to {}", jdaChecker.latest.version)
        }
    }

    private fun checkLatestJDAKtxVersion() {
        val changed = jdaKtxChecker.checkVersion()
        if (changed) {
            logger.info("JDA-KTX version changed")
            jdaKtxChecker.saveVersion()
            logger.info("JDA-KTX version updated to {}", jdaKtxChecker.latest.version)
        }
    }

    private fun checkLatestLavaPlayerVersion() {
        val changed = lavaPlayerChecker.checkVersion()
        if (changed) {
            logger.info("LavaPlayer version changed")
            lavaPlayerChecker.saveVersion()
            logger.info("LavaPlayer version updated to {}", lavaPlayerChecker.latest.version)
        }
    }

    //TODO use coroutines in version checkers / HTTP
    private fun checkLatestJDAVersionFromBC() {
        val changed = jdaVersionFromBCChecker.checkVersion()
        if (changed) {
            logger.info("BotCommands's JDA version changed")
            jdaVersionFromBCChecker.saveVersion()
            logger.info("BotCommands's JDA version updated to {}", jdaVersionFromBCChecker.latest.version)
        }
    }

    private suspend fun checkLatestBCVersion(context: BContext) {
        val changed = bcChecker.checkVersion()

        if (changed) {
            logger.info("BotCommands version changed")

            bcChecker.latest.downloadMavenJavadoc().withTemporaryFile { javadocPath ->
                logger.trace("Extracting BC javadocs")
                VersionsUtils.replaceWithZipContent(javadocPath, bcDocsFolder, "html")
            }

            bcChecker.latest.downloadMavenSources().withTemporaryFile { javadocPath ->
                logger.trace("Extracting BC sources")
                VersionsUtils.extractZip(javadocPath, bcDocsFolder, "java")
            }

            logger.trace("Invalidating BotCommands index")
            docIndexMap.refreshAndInvalidateIndex(DocSourceType.BOT_COMMANDS, ReindexData())
            for (handlerName in CommonDocsHandlers.AUTOCOMPLETE_NAMES) {
                context.invalidateAutocompleteCache(handlerName)
            }

            bcChecker.saveVersion()

            logger.info("BotCommands version updated to {}", bcChecker.latest.version)
        }
    }

    val latestBotCommandsVersion: ArtifactInfo
        get() = bcChecker.latest
    val jdaVersionFromBotCommands: ArtifactInfo
        get() = jdaVersionFromBCChecker.latest
    val latestJDAVersion: ArtifactInfo
        get() = jdaChecker.latest
    val latestJDAKtxVersion: ArtifactInfo
        get() = jdaKtxChecker.latest
    val latestLavaPlayerVersion: ArtifactInfo
        get() = lavaPlayerChecker.latest

    companion object {
        private val logger = KotlinLogging.logger { }
    }
}
