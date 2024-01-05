package com.freya02.bot.versioning.jitpack.jdafork

import com.freya02.bot.config.Config
import com.freya02.bot.config.Data
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.gson.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
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

private typealias BranchLabel = String
private typealias BranchSha = String

object JDAFork {
    data class BranchIdentifier(val forkBotName: String, val forkRepoName: String, val forkedBranchName: String)

    private val logger = KotlinLogging.logger { }
    private val config = Config.instance.pullUpdater
    private val forkPath = Data.jdaForkPath

    private val client = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            gson {  }
        }
    }
    private val mutex = Mutex()
    val isRunning: Boolean get() = mutex.isLocked

    private val latestHeadSha: MutableMap<BranchLabel, BranchSha> = hashMapOf()
    private val latestBaseSha: MutableMap<BranchLabel, BranchSha> = hashMapOf()

    //TODO use LibraryType
    //TODO replace BranchIdentifier with GithubBranch (may save doing an extra request for data we could already have)
    suspend fun requestUpdate(repository: String, prNumber: Int): Result<BranchIdentifier> = runCatching {
        if (repository != "JDA") {
            fail(JDAForkException.ExceptionType.UNSUPPORTED_LIBRARY, "Only JDA is supported")
        }

        mutex.withLock {
            init()

            val pullRequest: PullRequest = client.get("https://api.github.com/repos/discord-jda/JDA/pulls/$prNumber") {
                header("Accept", "applications/vnd.github.v3+json")
            }.also {
                if (it.status == HttpStatusCode.NotFound) {
                    fail(JDAForkException.ExceptionType.PR_NOT_FOUND, "Pull request not found")
                } else if (!it.status.isSuccess()) {
                    fail(JDAForkException.ExceptionType.UNKNOWN_ERROR, "Error while getting pull request")
                }
            }.body()

            if (pullRequest.merged) {
                //Skip merged PRs
                pullRequest.toBranchIdentifier()
            } else if (pullRequest.mergeable == false) {
                //Skip PRs with conflicts
                fail(JDAForkException.ExceptionType.PR_UPDATE_FAILURE, "Head branch cannot be updated")
            } else if (latestHeadSha[pullRequest.head.label] == pullRequest.head.sha && latestBaseSha[pullRequest.base.label] == pullRequest.base.sha) {
                //Prevent unnecessary updates by checking if the latest SHA is the same on the remote
                pullRequest.toBranchIdentifier()
            } else {
                val branchIdentifier = doUpdate(pullRequest)

                latestHeadSha[pullRequest.head.label] = pullRequest.head.sha
                latestBaseSha[pullRequest.base.label] = pullRequest.base.sha

                branchIdentifier
            }
        }
    }

    private fun fail(type: JDAForkException.ExceptionType, message: String): Nothing =
        throw JDAForkException(type, message)

    private fun PullRequest.toBranchIdentifier(): BranchIdentifier {
        return BranchIdentifier(config.forkBotName, config.forkRepoName, head.toForkedBranchName())
    }

    private suspend fun doUpdate(pullRequest: PullRequest): BranchIdentifier {
        //JDA repo most likely
        val base = pullRequest.base
        val baseBranchName = base.branchName
        val baseRepo = base.repo.name
        val baseRemoteName = base.user.userName

        //The PR author's repo
        val head = pullRequest.head
        val headBranchName = head.branchName
        val headRepo = head.repo.name
        val headRemoteName = head.user.userName

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
                fail(JDAForkException.ExceptionType.HEAD_REF_NOT_FOUND, "Head reference '$headRemoteReference' was not found")
            }
            fail(JDAForkException.ExceptionType.UNKNOWN_ERROR, "Error while switching to head branch")
        }

        //Merge base branch into remote branch
        val baseRemoteReference = "$baseRemoteName/$baseBranchName"
        try {
            runProcess(forkPath, "git", "merge", baseRemoteReference)
        } catch (e: ProcessException) {
            if (e.errorOutput.startsWith("fatal: invalid reference")) {
                fail(JDAForkException.ExceptionType.BASE_REF_NOT_FOUND, "Base reference '$baseRemoteReference' was not found")
            }
            fail(JDAForkException.ExceptionType.UNKNOWN_ERROR, "Error while switching to base branch")
        }

        //Publish result on our fork
        // Force push is used as the bot takes the remote head branch instead of reusing the local one,
        // meaning the remote branch would always be incompatible on the 2nd update
        runProcess(forkPath, "git", "push", "--force", "origin")

        return pullRequest.toBranchIdentifier()
    }

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

    private fun PullRequest.Branch.toForkedBranchName() = "${user.userName}/$branchName"

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

        return@withContext outputStream.toByteArray().decodeToString()
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