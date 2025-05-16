package dev.freya02.doxxy.bot.versioning

import dev.freya02.doxxy.bot.commands.slash.docs.CommonDocsHandlers
import dev.freya02.doxxy.bot.docs.DocIndexMap
import dev.freya02.doxxy.bot.docs.DocSourceType
import dev.freya02.doxxy.bot.docs.index.ReindexData
import dev.freya02.doxxy.bot.docs.javadocDirectory
import dev.freya02.doxxy.bot.utils.Utils.withTemporaryFile
import dev.freya02.doxxy.bot.versioning.VersionsUtils.downloadMavenJavadoc
import dev.freya02.doxxy.bot.versioning.VersionsUtils.downloadMavenSources
import dev.freya02.doxxy.bot.versioning.github.CommitHash
import dev.freya02.doxxy.bot.versioning.github.GithubClient
import dev.freya02.doxxy.bot.versioning.maven.DependencyVersionChecker
import dev.freya02.doxxy.bot.versioning.maven.MavenVersionChecker
import dev.freya02.doxxy.bot.versioning.maven.RepoType
import io.github.freya022.botcommands.api.commands.application.ApplicationCommandsContext
import io.github.freya022.botcommands.api.core.annotations.BEventListener
import io.github.freya022.botcommands.api.core.events.InjectedJDAEvent
import io.github.freya022.botcommands.api.core.service.annotations.BService
import io.github.freya022.botcommands.api.core.utils.namedDefaultScope
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

private val logger = KotlinLogging.logger { }

@BService
class Versions(
    private val applicationCommandsContext: ApplicationCommandsContext,
    private val docIndexMap: DocIndexMap,
    private val versionsRepository: VersionsRepository,
    private val githubClient: GithubClient,
) {
    private val bcChecker =
        MavenVersionChecker(versionsRepository.getInitialVersion(LibraryType.BOT_COMMANDS), RepoType.MAVEN)
    val latestBotCommandsVersion: ArtifactInfo
        get() = bcChecker.latest.artifactInfo

    private val bcJdaVersionChecker =
        DependencyVersionChecker(
            versionsRepository.getInitialVersion(LibraryType.JDA, "freya022-botcommands-release"),
            targetArtifactId = "JDA"
        ) { bcChecker.latest.artifactInfo.toMavenUrl(FileType.POM) }
    val jdaVersionFromBotCommands: ArtifactInfo
        get() = bcJdaVersionChecker.latest.artifactInfo

    private val jdaChecker: MavenVersionChecker =
        MavenVersionChecker(versionsRepository.getInitialVersion(LibraryType.JDA), RepoType.MAVEN)
    val latestJDAVersion: ArtifactInfo
        get() = jdaChecker.latest.artifactInfo

    private val jdaKtxChecker: MavenVersionChecker =
        MavenVersionChecker(versionsRepository.getInitialVersion(LibraryType.JDA_KTX), RepoType.MAVEN)
    val latestJDAKtxVersion: ArtifactInfo
        get() = jdaKtxChecker.latest.artifactInfo

    private val lavaPlayerChecker: MavenVersionChecker =
        MavenVersionChecker(versionsRepository.getInitialVersion(LibraryType.LAVA_PLAYER), RepoType.MAVEN)
    val latestLavaPlayerVersion: ArtifactInfo
        get() = lavaPlayerChecker.latest.artifactInfo

    @BEventListener(mode = BEventListener.RunMode.ASYNC, timeout = -1)
    suspend fun initUpdateLoop(event: InjectedJDAEvent) {
        val scope = namedDefaultScope("Version checker", 1)
        scope.scheduleWithFixedDelay(30.minutes, "BC", ::checkLatestBCVersion)
        scope.scheduleWithFixedDelay(30.minutes, "JDA", ::checkLatestJDAVersion)
        scope.scheduleWithFixedDelay(30.minutes, "JDA-KTX", ::checkLatestJDAKtxVersion)
        scope.scheduleWithFixedDelay(30.minutes, "LavaPlayer", ::checkLatestLavaPlayerVersion)

        //First index for Java's docs, may take some time
        if (docIndexMap[DocSourceType.JAVA].getClassDoc("Object") == null) {
            docIndexMap[DocSourceType.JAVA].reindex(ReindexData())

            //Once java's docs are indexed, invalidate caches if the user had time to use the commands before docs were loaded
            for (autocompleteName in CommonDocsHandlers.AUTOCOMPLETE_NAMES) {
                applicationCommandsContext.invalidateAutocompleteCache(autocompleteName)
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
            val sourceUrl = githubClient.getLatestReleaseHash("discord-jda", "JDA")
                ?.let { hash -> "https://github.com/discord-jda/JDA/blob/${hash.hash}/src/main/java/" }
                ?: return logger.debug { "Ignoring new JDA version (${jdaChecker.latest}) from Maven Central as no GitHub release could be retrieved" }

            if (sourceUrl == versionsRepository.findByName(LibraryType.JDA, classifier = null)?.sourceUrl)
                return logger.debug { "Ignoring new JDA version (${jdaChecker.latest}) from Maven Central as the GitHub release hasn't been made" }

            logger.info { "JDA version changed" }

            logger.trace { "Downloading JDA javadocs" }
            val jdaDocsFolder = DocSourceType.JDA.javadocDirectory
            jdaChecker.latest.artifactInfo.downloadMavenJavadoc().withTemporaryFile { tempZip ->
                logger.trace { "Extracting JDA javadocs" }
                VersionsUtils.replaceWithZipContent(tempZip, jdaDocsFolder, "html")
            }

            jdaChecker.latest.artifactInfo.downloadMavenSources().withTemporaryFile { tempZip ->
                logger.trace { "Extracting JDA sources" }
                VersionsUtils.extractZip(tempZip, jdaDocsFolder, "java")
            }

            logger.trace { "Invalidating JDA index" }
            docIndexMap[DocSourceType.JDA].reindex(ReindexData(sourceUrl))
            for (handlerName in CommonDocsHandlers.AUTOCOMPLETE_NAMES) {
                applicationCommandsContext.invalidateAutocompleteCache(handlerName)
            }

            jdaChecker.save(versionsRepository, sourceUrl = sourceUrl)

            logger.info { "JDA version updated to ${jdaChecker.latest.version}" }
        }
    }

    private suspend fun checkLatestJDAKtxVersion() {
        val changed = jdaKtxChecker.checkVersion()
        if (changed) {
            logger.info { "JDA-KTX version changed" }
            jdaKtxChecker.save(versionsRepository)
            logger.info { "JDA-KTX version updated to ${jdaKtxChecker.latest.version}" }
        }
    }

    private suspend fun checkLatestLavaPlayerVersion() {
        val changed = lavaPlayerChecker.checkVersion()
        if (changed) {
            logger.info { "LavaPlayer version changed" }
            lavaPlayerChecker.save(versionsRepository)
            logger.info { "LavaPlayer version updated to ${lavaPlayerChecker.latest.version}" }
        }
    }

    private suspend fun checkLatestBCVersion() {
        val changed = bcChecker.checkVersion()

        if (changed) {
            logger.info { "BotCommands version changed" }

            bcChecker.save(versionsRepository)
            bcJdaVersionChecker.checkVersion()
            bcJdaVersionChecker.save(versionsRepository)

            logger.info { "BotCommands version updated to ${bcChecker.latest.version}" }
        }
    }

    private suspend fun GithubClient.getLatestReleaseHash(owner: String, repo: String): CommitHash? {
        val latestRelease = getLatestRelease(owner, repo) ?: return null
        val latestReleaseTag = getAllTags(owner, repo).firstOrNull { tag -> tag.name == latestRelease.tagName }
            ?: return null

        return latestReleaseTag.commit.sha
    }
}
