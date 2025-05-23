package dev.freya02.doxxy.bot.versioning.jitpack.pullupdater

import dev.freya02.doxxy.bot.config.Config
import dev.freya02.doxxy.bot.versioning.LibraryType
import dev.freya02.doxxy.common.Directories
import dev.freya02.doxxy.github.client.GithubClient
import dev.freya02.doxxy.github.client.data.Branch
import dev.freya02.doxxy.github.client.data.PullRequest
import dev.freya02.doxxy.github.client.utils.CommitHash
import io.github.freya022.botcommands.api.core.service.annotations.BService
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlin.io.path.moveTo
import kotlin.io.path.name
import kotlin.io.path.notExists

@BService
class PullUpdater(
    private val githubClient: GithubClient,
) {
    private data class CacheKey(val headLabel: String, val baseLabel: String)
    private data class CacheValue(val headSha: CommitHash, val baseSha: CommitHash, val forkedGithubBranch: UpdatedBranch)

    private val logger = KotlinLogging.logger { }
    private val config = Config.config.pullUpdater
    private val forkPath = Directories.jdaFork

    private val mutex = Mutex()
    val isRunning: Boolean get() = mutex.isLocked

    private val cache: MutableMap<CacheKey, CacheValue> = hashMapOf()
    private var latestCommitHash: CommitHash? = null

    suspend fun tryUpdate(libraryType: LibraryType, pullRequest: PullRequest): Result<UpdatedBranch> = runCatching {
        if (libraryType != LibraryType.JDA) {
            fail(PullUpdateException.ExceptionType.UNSUPPORTED_LIBRARY, "Only JDA is supported")
        }

        mutex.withLock {
            init()

            if (pullRequest.mergeable != true) {
                //Skip PRs with conflicts
                fail(PullUpdateException.ExceptionType.PR_UPDATE_FAILURE, "Head branch cannot be updated")
            } else {
                whenUpdateRequired(libraryType, pullRequest) {
                    val mergeCommitHash = doUpdate(pullRequest)
                    getForkedGithubBranch(pullRequest, mergeCommitHash)
                }
            }
        }
    }

    private suspend fun whenUpdateRequired(libraryType: LibraryType, pullRequest: PullRequest, onUpdate: suspend () -> UpdatedBranch): UpdatedBranch {
        val latestHash = githubClient
            .getCommits(libraryType.githubOwnerName, libraryType.githubRepoName, perPage = 1)
            .first()
            .sha

        fun createCacheKey(): CacheKey = CacheKey(pullRequest.head.label, pullRequest.base.label)

        suspend fun update(): UpdatedBranch {
            val newBranch = onUpdate()
            this.latestCommitHash = latestHash
            cache[createCacheKey()] = CacheValue(pullRequest.head.sha, pullRequest.base.sha, newBranch)
            return newBranch
        }

        // If JDA main branch has been updated
        if (this.latestCommitHash != latestHash)
            return update()

        // Check if the base sha (where the PR starts from) and head sha (latest PR commit) correspond
        val cacheKey = createCacheKey()
        val cacheValue = cache[cacheKey] ?: return update()

        val isCacheValid = cacheValue.baseSha == pullRequest.base.sha
                && cacheValue.headSha == pullRequest.head.sha
        return when {
            isCacheValid -> cacheValue.forkedGithubBranch
            else -> update()
        }
    }

    private fun getForkedGithubBranch(pr: PullRequest, mergeCommitHash: CommitHash): UpdatedBranch {
        return UpdatedBranch(config.forkBotName, config.forkRepoName, pr.head.toForkedBranchName(), mergeCommitHash)
    }

    private suspend fun doUpdate(pullRequest: PullRequest): CommitHash {
        //JDA repo most likely
        val base = pullRequest.base
        val baseBranchName = base.branchName
        val baseRepo = base.repo.name
        val baseRemoteName = base.user.login

        //The PR author's repo
        val head = pullRequest.head
        val headBranchName = head.branchName
        val headRepo = head.repo.name
        val headRemoteName = head.user.login

        //Add remote
        val remotes = runProcess(forkPath, "git", "remote").trim().lineSequence().toMutableList()
        if (baseRemoteName !in remotes) {
            runProcess(forkPath, "git", "remote", "add", baseRemoteName, "https://github.com/$baseRemoteName/$baseRepo")
            remotes += baseRemoteName
        }
        if (headRemoteName !in remotes) {
            runProcess(forkPath, "git", "remote", "add", headRemoteName, "https://github.com/$headRemoteName/$headRepo")
            remotes += headRemoteName
        }

        //Fetch base and head repo
        runProcess(forkPath, "git", "fetch", baseRemoteName)
        runProcess(forkPath, "git", "fetch", headRemoteName)

        //Use remote branch
        val headRemoteReference = "refs/remotes/$headRemoteName/$headBranchName"
        try {
            runProcess(
                forkPath,
                "git",
                "switch",
                "--force-create",
                head.toForkedBranchName(),
                headRemoteReference
            )
        } catch (e: ProcessException) {
            if (e.errorOutput.startsWith("fatal: invalid reference")) {
                fail(PullUpdateException.ExceptionType.HEAD_REF_NOT_FOUND, "Head reference '$headRemoteReference' was not found")
            }
            fail(PullUpdateException.ExceptionType.UNKNOWN_ERROR, "Error while switching to head branch")
        }

        //Merge base branch into remote branch
        val baseRemoteReference = "$baseRemoteName/$baseBranchName"
        try {
            runProcess(forkPath, "git", "merge", baseRemoteReference)
        } catch (e: ProcessException) {
            if (e.errorOutput.startsWith("fatal: invalid reference")) {
                fail(PullUpdateException.ExceptionType.BASE_REF_NOT_FOUND, "Base reference '$baseRemoteReference' was not found")
            }
            fail(PullUpdateException.ExceptionType.UNKNOWN_ERROR, "Error while switching to base branch")
        }

        //Publish the result on our fork
        // Force push is used as the bot takes the remote head branch instead of reusing the local one,
        // meaning the remote branch would always be incompatible on the 2nd update
        runProcess(forkPath, "git", "push", "--force", "origin")

        return CommitHash(runProcess(forkPath, "git", "rev-parse", "HEAD"))
    }

    private fun fail(type: PullUpdateException.ExceptionType, message: String): Nothing =
        throw PullUpdateException(type, message)

    private suspend fun init() {
        if (forkPath.notExists()) {
            val forkPathTmp = forkPath.resolveSibling("JDA-Fork-tmp")
            runProcess(
                workingDirectory = forkPathTmp.parent,
                "git", "clone", "https://${config.gitToken}@github.com/${config.forkBotName}/${config.forkRepoName}", forkPathTmp.name
            )
            runProcess(
                workingDirectory = forkPathTmp,
                "git", "config", "--local", "user.name", config.gitName
            )
            runProcess(
                workingDirectory = forkPathTmp,
                "git", "config", "--local", "user.email", config.gitEmail
            )

            //Disable signing just in case
            runProcess(
                workingDirectory = forkPathTmp,
                "git", "config", "--local", "commit.gpgsign", "false"
            )
            runProcess(
                workingDirectory = forkPathTmp,
                "git", "config", "--local", "tag.gpgsign", "false"
            )

            forkPathTmp.moveTo(forkPath, StandardCopyOption.ATOMIC_MOVE)
        }
    }

    private fun Branch.toForkedBranchName() = "${user.login}/$branchName"

    private suspend fun runProcess(workingDirectory: Path, vararg command: String): String = withContext(Dispatchers.IO) {
        val process = ProcessBuilder(command.asList())
            .directory(workingDirectory.toFile())
            .start()

        val outputStream = ByteArrayOutputStream()
        val errorStream = ByteArrayOutputStream()
        val exitCode = coroutineScope {
            launch { redirectStream(outputStream, process.inputStream) }
            launch { redirectStream(errorStream, process.errorStream) }

            process.waitFor()
        }

        if (exitCode != 0) {
            val outputString = outputStream.toByteArray().decodeToString()
            when {
                outputString.isNotBlank() -> logger.warn { "Output:\n$outputString" }
                else -> logger.warn { "No output" }
            }

            val errorString = errorStream.toByteArray().decodeToString()
            when {
                errorString.isNotBlank() -> logger.error { "Error output:\n$errorString" }
                else -> logger.warn { "No error output" }
            }

            throw ProcessException(exitCode, errorString, "Process exited with code $exitCode: ${command.joinToString(" ") { if (it.contains("github_pat_")) "[bot_repo]" else it }}")
        }

        outputStream.toByteArray().decodeToString()
    }

    private fun redirectStream(arrayStream: ByteArrayOutputStream, processStream: InputStream) {
        arrayStream.bufferedWriter().use { writer ->
            processStream.bufferedReader().use { reader ->
                var readLine: String?
                while (reader.readLine().also { readLine = it } != null) {
                    writer.append(readLine + System.lineSeparator())
                }
            }
        }
    }
}