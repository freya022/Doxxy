package dev.freya02.doxxy.bot.versioning.jitpack

import dev.freya02.doxxy.bot.utils.UpdateCountdown
import dev.freya02.doxxy.bot.versioning.LibraryType
import dev.freya02.doxxy.github.client.GithubClient
import dev.freya02.doxxy.github.client.data.Branches
import io.github.freya022.botcommands.api.core.service.annotations.BService
import io.github.freya022.botcommands.api.core.utils.enumMapOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Duration.Companion.minutes

@BService
class JitpackBranchService(
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

    private val updatedBranchesMapMutex = Mutex()
    private val updatedBranchesMap: MutableMap<LibraryType, UpdatedBranchMap> = enumMapOf()

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
}
