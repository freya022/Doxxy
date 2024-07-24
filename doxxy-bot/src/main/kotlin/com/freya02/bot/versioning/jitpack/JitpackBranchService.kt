package com.freya02.bot.versioning.jitpack

import com.freya02.bot.commands.slash.versioning.SlashJitpack
import com.freya02.bot.versioning.*
import com.freya02.bot.versioning.github.GithubBranch
import com.freya02.bot.versioning.github.GithubBranchMap
import com.freya02.bot.versioning.github.GithubUtils
import com.freya02.bot.versioning.github.UpdateCountdown
import com.freya02.bot.versioning.maven.DependencyVersionChecker
import io.github.freya022.botcommands.api.commands.application.ApplicationCommandsContext
import io.github.freya022.botcommands.api.core.service.annotations.BService
import io.github.freya022.botcommands.api.core.utils.enumMapOf
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Duration.Companion.minutes

private typealias BranchName = String

@BService
class JitpackBranchService(
    private val applicationCommandsContext: ApplicationCommandsContext,
    private val versionsRepository: VersionsRepository
) {
    private sealed class UpdatedValue<T : Any> {
        private val updateCountdown = UpdateCountdown(1.minutes)

        private lateinit var value: T

        abstract suspend fun update(): T

        suspend fun get(): T {
            updateCountdown.onUpdate {
                value = update()
            }
            return value
        }
    }

    private inner class UpdatedBranchMap(private val libraryType: LibraryType) : UpdatedValue<GithubBranchMap>() {
        override suspend fun update(): GithubBranchMap {
            val map: Map<String, GithubBranch> = GithubUtils.getBranches(libraryType.githubOwnerName, libraryType.githubRepoName).associateBy { it.branchName }
            val defaultBranchName = GithubUtils.getDefaultBranchName(libraryType.githubOwnerName, libraryType.githubRepoName)
            val defaultBranch = map[defaultBranchName]!!
            return GithubBranchMap(defaultBranch, map)
        }
    }

    private inner class UpdatedDependencyVersionChecker(private val checker: DependencyVersionChecker) : UpdatedValue<LibraryVersion>() {
        override suspend fun update(): LibraryVersion {
            checker.checkVersion()
            applicationCommandsContext.invalidateAutocompleteCache(SlashJitpack.PR_NUMBER_AUTOCOMPLETE_NAME)
            checker.save(versionsRepository)
            return checker.latest
        }
    }

    private val updatedBranchesMapMutex = Mutex()
    private val updatedBranchesMap: MutableMap<LibraryType, UpdatedBranchMap> = enumMapOf()
    private val versionCheckerMapMutex = Mutex()
    private val versionCheckerMap: MutableMap<BranchName, UpdatedDependencyVersionChecker> = hashMapOf()

    suspend fun getBranchMap(libraryType: LibraryType): GithubBranchMap = updatedBranchesMapMutex.withLock {
        val updatedBranchMap = updatedBranchesMap.computeIfAbsent(libraryType, ::UpdatedBranchMap)
        updatedBranchMap.get()
    }

    suspend fun getBranch(libraryType: LibraryType, branchName: String?): GithubBranch? {
        val githubBranchMap = getBranchMap(libraryType)
        return when (branchName) {
            null -> githubBranchMap.defaultBranch
            else -> githubBranchMap.branches[branchName]
        }
    }

    suspend fun getUsedJDAVersionFromBranch(libraryType: LibraryType, branch: GithubBranch): ArtifactInfo = versionCheckerMapMutex.withLock {
        val jdaVersionChecker = versionCheckerMap.getOrPut(branch.branchName) {
            val latest = versionsRepository.getInitialVersion(libraryType, branch.toVersionClassifier())
            val checker = DependencyVersionChecker(latest, "JDA") {
                "https://raw.githubusercontent.com/${branch.ownerName}/${branch.repoName}/${branch.branchName}/pom.xml"
            }
            UpdatedDependencyVersionChecker(checker)
        }

        jdaVersionChecker.get().artifactInfo
    }

    private fun GithubBranch.toVersionClassifier() = "$ownerName-$repoName-$branchName"
}
