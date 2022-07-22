package com.freya02.bot.versioning

import com.freya02.bot.Main
import com.freya02.bot.commands.slash.docs.CommonDocsHandlers
import com.freya02.bot.docs.DocIndexMap
import com.freya02.bot.docs.index.ReindexData
import com.freya02.bot.utils.Utils.withTemporaryFile
import com.freya02.bot.versioning.VersionsUtils.downloadMavenJavadoc
import com.freya02.bot.versioning.VersionsUtils.downloadMavenSources
import com.freya02.bot.versioning.github.GithubUtils
import com.freya02.bot.versioning.maven.MavenBranchProjectDependencyVersionChecker
import com.freya02.bot.versioning.maven.MavenVersionChecker
import com.freya02.bot.versioning.maven.RepoType
import com.freya02.botcommands.api.BContext
import com.freya02.botcommands.api.Logging
import com.freya02.docs.DocSourceType
import kotlinx.coroutines.runBlocking
import java.io.IOException
import java.nio.file.Path
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

private val LOGGER = Logging.getLogger()
private val lastKnownBotCommandsPath: Path = Main.LAST_KNOWN_VERSIONS_FOLDER_PATH.resolve("BC.txt")
private val lastKnownJDAFromBCPath: Path = Main.LAST_KNOWN_VERSIONS_FOLDER_PATH.resolve("JDA_from_BC.txt")
private val lastKnownJDA4Path: Path = Main.LAST_KNOWN_VERSIONS_FOLDER_PATH.resolve("JDA4.txt")
private val lastKnownJDA5Path: Path = Main.LAST_KNOWN_VERSIONS_FOLDER_PATH.resolve("JDA5.txt")
private val JDA_DOCS_FOLDER: Path = Main.JAVADOCS_PATH.resolve("JDA")
private val BC_DOCS_FOLDER: Path = Main.JAVADOCS_PATH.resolve("BotCommands")

class Versions(private val docIndexMap: DocIndexMap) {
    private val bcChecker =
        MavenVersionChecker(lastKnownBotCommandsPath, RepoType.MAVEN, "io.github.freya022", "BotCommands")
    private val jdaVersionFromBCChecker: MavenBranchProjectDependencyVersionChecker =
        MavenBranchProjectDependencyVersionChecker(lastKnownJDAFromBCPath, "freya022", "BotCommands", "JDA", "master")
    private val jda4Checker: MavenVersionChecker =
        MavenVersionChecker(lastKnownJDA4Path, RepoType.M2, "net.dv8tion", "JDA")
    private val jda5Checker: MavenVersionChecker =
        MavenVersionChecker(lastKnownJDA5Path, RepoType.MAVEN, "net.dv8tion", "JDA")

    @Throws(IOException::class)
    suspend fun initUpdateLoop(context: BContext?) {
        val scheduledExecutorService = Executors.newSingleThreadScheduledExecutor()

        scheduledExecutorService.scheduleWithFixedDelay({ checkLatestBCVersion(context) }, 0, 30, TimeUnit.MINUTES)
        scheduledExecutorService.scheduleWithFixedDelay({ checkLatestJDAVersionFromBC() }, 0, 30, TimeUnit.MINUTES)
        scheduledExecutorService.scheduleWithFixedDelay({ checkLatestJDA4Version() }, 0, 30, TimeUnit.MINUTES)
        scheduledExecutorService.scheduleWithFixedDelay({ checkLatestJDA5Version(context) }, 0, 30, TimeUnit.MINUTES)

        //First index for Java's docs, may take some time
        if (docIndexMap[DocSourceType.JAVA]!!.getClassDoc("Object") == null) {
            docIndexMap[DocSourceType.JAVA]!!.reindex(ReindexData())

            //Once java's docs are indexed, invalidate caches if the user had time to use the commands before docs were loaded
            for (autocompleteName in CommonDocsHandlers.AUTOCOMPLETE_NAMES) {
                context?.invalidateAutocompletionCache(autocompleteName)
            }
        }
    }

    private fun checkLatestJDA5Version(context: BContext?): Boolean {
        try {
            val changed = jda5Checker.checkVersion()

            if (changed) {
                LOGGER.info("JDA 5 version changed")

                LOGGER.trace("Downloading JDA 5 javadocs")
                jda5Checker.latest.downloadMavenJavadoc().withTemporaryFile { tempZip ->
                    LOGGER.trace("Extracting JDA 5 javadocs")
                    VersionsUtils.replaceWithZipContent(tempZip, JDA_DOCS_FOLDER, "html")
                }

                jda5Checker.latest.downloadMavenSources().withTemporaryFile { tempZip ->
                    LOGGER.trace("Extracting JDA 5 sources")
                    VersionsUtils.extractZip(tempZip, JDA_DOCS_FOLDER, "java")
                }

                val sourceUrl = GithubUtils.getLatestReleaseHash("DV8FromTheWorld", "JDA")
                    ?.let { hash -> "https://github.com/DV8FromTheWorld/JDA/blob/${hash.hash}/src/main/java/" }

                LOGGER.trace("Invalidating JDA 5 index")
                runBlocking { docIndexMap.refreshAndInvalidateIndex(DocSourceType.JDA, ReindexData(sourceUrl)) }
                for (handlerName in CommonDocsHandlers.AUTOCOMPLETE_NAMES) {
                    context?.invalidateAutocompletionCache(handlerName)
                }

                jda5Checker.saveVersion()

                LOGGER.info("JDA 5 version updated to {}", jda5Checker.latest.version)
            }
            return changed
        } catch (e: IOException) {
            LOGGER.error("An exception occurred while retrieving versions", e)
        }

        return false
    }

    private fun checkLatestJDA4Version() {
        try {
            val changed = jda4Checker.checkVersion()
            if (changed) {
                LOGGER.info("JDA 4 version changed")
                jda4Checker.saveVersion()
                LOGGER.info("JDA 4 version updated to {}", jda4Checker.latest.version)
            }
        } catch (e: IOException) {
            LOGGER.error("An exception occurred while retrieving versions", e)
        }
    }

    private fun checkLatestJDAVersionFromBC() {
        try {
            val changed = jdaVersionFromBCChecker.checkVersion()
            if (changed) {
                LOGGER.info("BotCommands's JDA version changed")
                jdaVersionFromBCChecker.saveVersion()
                LOGGER.info("BotCommands's JDA version updated to {}", jdaVersionFromBCChecker.latest.version)
            }
        } catch (e: IOException) {
            LOGGER.error("An exception occurred while retrieving versions", e)
        }
    }

    fun checkLatestBCVersion(context: BContext?): Boolean {
        try {
            val changed = bcChecker.checkVersion()

            if (changed) {
                LOGGER.info("BotCommands version changed")

                bcChecker.latest.downloadMavenJavadoc().withTemporaryFile { javadocPath ->
                    LOGGER.trace("Extracting BC javadocs")
                    VersionsUtils.replaceWithZipContent(javadocPath, BC_DOCS_FOLDER, "html")
                }

                bcChecker.latest.downloadMavenSources().withTemporaryFile { javadocPath ->
                    LOGGER.trace("Extracting BC sources")
                    VersionsUtils.extractZip(javadocPath, BC_DOCS_FOLDER, "java")
                }

                LOGGER.trace("Invalidating BotCommands index")
                runBlocking { docIndexMap.refreshAndInvalidateIndex(DocSourceType.BOT_COMMANDS, ReindexData()) }
                for (handlerName in CommonDocsHandlers.AUTOCOMPLETE_NAMES) {
                    context?.invalidateAutocompletionCache(handlerName)
                }

                bcChecker.saveVersion()

                LOGGER.info("BotCommands version updated to {}", bcChecker.latest.version)
            }

            return changed
        } catch (e: IOException) {
            LOGGER.error("An exception occurred while retrieving versions", e)
        } catch (e: InterruptedException) {
            LOGGER.error("An exception occurred while retrieving versions", e)
        }

        return false
    }

    val latestBotCommandsVersion: ArtifactInfo
        get() = bcChecker.latest
    val jdaVersionFromBotCommands: ArtifactInfo
        get() = jdaVersionFromBCChecker.latest
    val latestJDA4Version: ArtifactInfo
        get() = jda4Checker.latest
    val latestJDA5Version: ArtifactInfo
        get() = jda5Checker.latest
}