package dev.freya02.doxxy.bot.versioning.jitpack

import dev.freya02.doxxy.bot.commands.slash.versioning.SlashJitpackPr
import dev.freya02.doxxy.bot.utils.UpdateCountdown
import dev.freya02.doxxy.bot.versioning.*
import dev.freya02.doxxy.bot.versioning.jitpack.pullupdater.UpdatedBranch
import dev.freya02.doxxy.bot.versioning.maven.DependencyVersionChecker
import dev.freya02.doxxy.github.client.GithubClient
import dev.freya02.doxxy.github.client.data.Branches
import io.github.freya022.botcommands.api.commands.application.ApplicationCommandsContext
import io.github.freya022.botcommands.api.core.service.annotations.BService
import io.github.freya022.botcommands.api.core.utils.enumMapOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Duration.Companion.minutes

private typealias BranchName = String

@BService
class JitpackBranchService(
    private val applicationCommandsContext: ApplicationCommandsContext,
    private val versionsRepository: VersionsRepository,
    private val githubClient: GithubClient,
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

    @JvmRecord
    data class GithubBranchMap(val defaultBranch: Branches.Branch, val branches: Map<String, Branches.Branch>)

    private inner class UpdatedBranchMap(private val libraryType: LibraryType) : UpdatedValue<GithubBranchMap>() {
        override suspend fun update(): GithubBranchMap {
            val branchByName = githubClient
                .getBranches(libraryType.githubOwnerName, libraryType.githubRepoName, perPage = 100)
                .toList()
                .associateBy { it.name }
            val defaultBranchName = githubClient
                .getRepository(libraryType.githubOwnerName, libraryType.githubRepoName)
                .defaultBranch
            val defaultBranch = branchByName[defaultBranchName]!!
            return GithubBranchMap(defaultBranch, branchByName)
        }
    }

    private inner class UpdatedDependencyVersionChecker(private val checker: DependencyVersionChecker) : UpdatedValue<LibraryVersion>() {
        override suspend fun update(): LibraryVersion {
            checker.checkVersion()
            applicationCommandsContext.invalidateAutocompleteCache(SlashJitpackPr.PR_NUMBER_AUTOCOMPLETE_NAME)
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

    suspend fun getBranch(libraryType: LibraryType, branchName: String?): Branches.Branch? {
        val githubBranchMap = getBranchMap(libraryType)
        return when (branchName) {
            null -> githubBranchMap.defaultBranch
            else -> githubBranchMap.branches[branchName]
        }
    }

    suspend fun getUsedJDAVersionFromBranch(branch: UpdatedBranch): ArtifactInfo = versionCheckerMapMutex.withLock {
        val jdaVersionChecker = versionCheckerMap.getOrPut(branch.branchName) {
            // Get back from DB the saved JDA version with the branch classifier (i.e., JDA from BC)
            val latest = versionsRepository.getInitialVersion(LibraryType.JDA, branch.toVersionClassifier())
            val checker = DependencyVersionChecker(latest, "JDA") {
                "https://raw.githubusercontent.com/${branch.ownerName}/${branch.repoName}/${branch.branchName}/pom.xml"
            }
            UpdatedDependencyVersionChecker(checker)
        }

        jdaVersionChecker.get().artifactInfo
    }

    private fun UpdatedBranch.toVersionClassifier() = "$ownerName-$repoName-$branchName"
}
