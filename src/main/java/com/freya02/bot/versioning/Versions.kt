package com.freya02.bot.versioning

import com.freya02.bot.Main
import com.freya02.bot.commands.slash.docs.CommonDocsHandlers
import com.freya02.bot.docs.DocIndexMap
import com.freya02.bot.utils.ProcessUtils
import com.freya02.bot.versioning.jitpack.JitpackUtils
import com.freya02.bot.versioning.jitpack.JitpackVersionChecker
import com.freya02.bot.versioning.maven.MavenProjectDependencyVersionChecker
import com.freya02.bot.versioning.maven.MavenUtils
import com.freya02.bot.versioning.maven.MavenVersionChecker
import com.freya02.bot.versioning.maven.RepoType
import com.freya02.botcommands.api.BContext
import com.freya02.botcommands.api.Logging
import com.freya02.docs.DocSourceType
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

private val LOGGER = Logging.getLogger()
private val lastKnownBotCommandsPath = Main.LAST_KNOWN_VERSIONS_FOLDER_PATH.resolve("BC.txt")
private val lastKnownJDAFromBCPath = Main.LAST_KNOWN_VERSIONS_FOLDER_PATH.resolve("JDA_from_BC.txt")
private val lastKnownJDA4Path = Main.LAST_KNOWN_VERSIONS_FOLDER_PATH.resolve("JDA4.txt")
private val lastKnownJDA5Path = Main.LAST_KNOWN_VERSIONS_FOLDER_PATH.resolve("JDA5.txt")
private val JDA_DOCS_FOLDER = Main.JAVADOCS_PATH.resolve("JDA")
private val BC_DOCS_FOLDER = Main.JAVADOCS_PATH.resolve("BotCommands")

class Versions {
    private val bcChecker: JitpackVersionChecker
    private val jdaVersionFromBCChecker: MavenProjectDependencyVersionChecker
    private val jda4Checker: MavenVersionChecker
    private val jda5Checker: MavenVersionChecker

    init {
        Files.createDirectories(Main.LAST_KNOWN_VERSIONS_FOLDER_PATH)

        bcChecker = JitpackVersionChecker(lastKnownBotCommandsPath, "freya022", "com.github.freya022", "BotCommands")
        jdaVersionFromBCChecker =
            MavenProjectDependencyVersionChecker(lastKnownJDAFromBCPath, "freya022", "BotCommands", "JDA")
        jda4Checker = MavenVersionChecker(lastKnownJDA4Path, RepoType.M2, "net.dv8tion", "JDA")
        jda5Checker = MavenVersionChecker(lastKnownJDA5Path, RepoType.MAVEN, "net.dv8tion", "JDA")
    }

    @Throws(IOException::class)
    fun initUpdateLoop(context: BContext?) {
        val scheduledExecutorService = Executors.newSingleThreadScheduledExecutor()

        //We need to check if the version has **not** changed between runs
        // If the version changed then it would have updated in the scheduled executor
        // But if the version didn't change then the docs wouldn't have been indexed
        // This is why we index them here, if no update is required
        if (!checkLatestBCVersion(context)) {
            //Load docs normally, version hasn't changed
            DocIndexMap.getInstance()[DocSourceType.BOT_COMMANDS]!!.reindex()
        }

        if (!checkLatestJDA5Version(context)) {
            //Load docs normally, version hasn't changed
            DocIndexMap.getInstance()[DocSourceType.JDA]!!.reindex()
        }

        scheduledExecutorService.scheduleWithFixedDelay({ checkLatestBCVersion(context) }, 30, 30, TimeUnit.MINUTES)
        scheduledExecutorService.scheduleWithFixedDelay({ checkLatestJDAVersionFromBC() }, 0, 30, TimeUnit.MINUTES)
        scheduledExecutorService.scheduleWithFixedDelay({ checkLatestJDA4Version() }, 0, 30, TimeUnit.MINUTES)
        scheduledExecutorService.scheduleWithFixedDelay({ checkLatestJDA5Version(context) }, 30, 30, TimeUnit.MINUTES)

        //First index for Java's docs, may take some time
        DocIndexMap.getInstance()[DocSourceType.JAVA]!!.reindex()

        //Once we loaded everything, invalidate caches if the user had time to use the commands before docs were loaded
        for (autocompleteName in CommonDocsHandlers.AUTOCOMPLETE_NAMES) {
            context?.invalidateAutocompletionCache(autocompleteName)
        }
    }

    private fun checkLatestJDA5Version(context: BContext?): Boolean {
        try {
            val changed = jda5Checker.checkVersion()

            if (changed) {
                LOGGER.info("JDA 5 version changed")

                LOGGER.debug("Downloading JDA 5 javadocs")
                val tempZip = Files.createTempFile("JDA5Docs", ".zip")
                MavenUtils.downloadMavenDocs(jda5Checker.getLatest(), tempZip)

                LOGGER.debug("Extracting JDA 5 javadocs")
                VersionsUtils.extractZip(tempZip, JDA_DOCS_FOLDER)
                Files.deleteIfExists(tempZip)

                LOGGER.debug("Invalidating JDA 5 index")
                DocIndexMap.refreshAndInvalidateIndex(DocSourceType.JDA)
                for (handlerName in CommonDocsHandlers.AUTOCOMPLETE_NAMES) {
                    context?.invalidateAutocompletionCache(handlerName)
                }

                jda5Checker.saveVersion()

                LOGGER.info("JDA 5 version updated to {}", jda5Checker.getLatest().version())
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
                LOGGER.info("JDA 4 version updated to {}", jda4Checker.getLatest().version())
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
                LOGGER.info("BotCommands's JDA version updated to {}", jdaVersionFromBCChecker.getLatest().version())
            }
        } catch (e: IOException) {
            LOGGER.error("An exception occurred while retrieving versions", e)
        }
    }

    private fun checkLatestBCVersion(context: BContext?): Boolean {
        try {
            val changed = bcChecker.checkVersion()

            if (changed) {
                LOGGER.info("BotCommands version changed")

                val BCRepoPath = Main.REPOS_PATH.resolve("BotCommands")
                val needClone = Files.notExists(BCRepoPath)
                if (needClone) {
                    LOGGER.debug("Cloning BC repo")
                    ProcessUtils.runAndWait("git clone https://github.com/freya022/BotCommands.git", Main.REPOS_PATH)
                } else {
                    LOGGER.debug("Fetching BC repo")
                    ProcessUtils.runAndWait("git fetch", BCRepoPath)
                }

                val latestBranch = bcChecker.latestBranch
                LOGGER.debug("Switching to BC branch {}", latestBranch.branchName())

                ProcessUtils.runAndWait("git checkout " + latestBranch.branchName(), BCRepoPath)
                if (!needClone) {
                    LOGGER.debug("Pulling changes from BC branch {}", latestBranch.branchName())
                    ProcessUtils.runAndWait("git pull", BCRepoPath)
                }

                LOGGER.debug("Running mvn javadoc:javadoc")
                if (System.getProperty("os.name").lowercase(Locale.getDefault()).contains("windows")) {
                    ProcessUtils.runAndWait("mvn.cmd javadoc:javadoc", BCRepoPath)
                } else {
                    ProcessUtils.runAndWait("mvn javadoc:javadoc", BCRepoPath)
                }

                val targetDocsFolder = BC_DOCS_FOLDER
                if (Files.exists(targetDocsFolder)) {
                    LOGGER.debug("Removing old BC docs at {}", targetDocsFolder)
                    for (path in Files.walk(targetDocsFolder).sorted(Comparator.reverseOrder()).toList()) {
                        Files.deleteIfExists(path)
                    }
                }

                val apiDocsPath = BCRepoPath.resolve("target").resolve("site").resolve("apidocs")
                LOGGER.debug("Moving docs from {} to {}", apiDocsPath, targetDocsFolder)
                for (sourcePath in Files.walk(apiDocsPath).use {
                    it
                        .filter { path: Path -> Files.isRegularFile(path) }
                        .filter { p: Path -> p.fileName.toString().endsWith("html") }
                }) {
                    val targetPath = targetDocsFolder.resolve(apiDocsPath.relativize(sourcePath).toString())
                    Files.createDirectories(targetPath.parent)
                    Files.move(sourcePath, targetPath, StandardCopyOption.ATOMIC_MOVE)
                }

                LOGGER.debug("Invalidating BotCommands index")
                DocIndexMap.refreshAndInvalidateIndex(DocSourceType.BOT_COMMANDS)
                for (handlerName in CommonDocsHandlers.AUTOCOMPLETE_NAMES) {
                    context?.invalidateAutocompletionCache(handlerName)
                }

                bcChecker.saveVersion()

                LOGGER.info("BotCommands version updated to {}", bcChecker.getLatest().version())

                JitpackUtils.triggerBuild(
                    bcChecker.getLatest().groupId(),
                    bcChecker.getLatest().artifactId(),
                    bcChecker.getLatest().version()
                )
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
        get() = bcChecker.getLatest()
    val jdaVersionFromBotCommands: ArtifactInfo
        get() = jdaVersionFromBCChecker.getLatest()
    val latestJDA4Version: ArtifactInfo
        get() = jda4Checker.getLatest()
    val latestJDA5Version: ArtifactInfo
        get() = jda5Checker.getLatest()
}