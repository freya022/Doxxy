package com.freya02.bot.versioning.jitpack

import com.freya02.bot.commands.slash.versioning.SlashJitpack
import com.freya02.bot.versioning.*
import com.freya02.bot.versioning.github.GithubBranch
import com.freya02.bot.versioning.github.GithubBranchMap
import com.freya02.bot.versioning.github.GithubUtils
import com.freya02.bot.versioning.github.UpdateCountdown
import com.freya02.bot.versioning.maven.DependencyVersionChecker
import io.github.freya022.botcommands.api.core.BContext
import io.github.freya022.botcommands.api.core.service.annotations.BService
import java.io.IOException
import java.util.*
import kotlin.time.Duration.Companion.minutes

@BService
class JitpackBranchService(
    private val context: BContext,
    private val versionsRepository: VersionsRepository
) {
    private val updateMap: MutableMap<LibraryType, UpdateCountdown> = EnumMap(LibraryType::class.java)
    private val branchMap: MutableMap<LibraryType, GithubBranchMap> = EnumMap(LibraryType::class.java)

    private val branchNameToJdaVersionChecker: MutableMap<String, DependencyVersionChecker> =
        Collections.synchronizedMap(hashMapOf())
    private val updateCountdownMap: MutableMap<String, UpdateCountdown> = HashMap()

    fun getBranchMap(libraryType: LibraryType): GithubBranchMap {
        val updateCountdown =
            updateMap.computeIfAbsent(libraryType) { UpdateCountdown(1.minutes) }

        synchronized(branchMap) {
            branchMap[libraryType].let { githubBranchMap: GithubBranchMap? ->
                if (githubBranchMap == null || updateCountdown.needsUpdate()) {
                    return retrieveBranchList(libraryType)
                        .also { updatedMap -> branchMap[libraryType] = updatedMap }
                }

                return githubBranchMap
            }
        }
    }

    fun getBranch(libraryType: LibraryType, branchName: String?): GithubBranch? {
        val githubBranchMap = getBranchMap(libraryType)
        return when (branchName) {
            null -> githubBranchMap.defaultBranch
            else -> githubBranchMap.branches[branchName]
        }
    }

    suspend fun getUsedJDAVersionFromBranch(libraryType: LibraryType, branch: GithubBranch): ArtifactInfo {
        val jdaVersionChecker = branchNameToJdaVersionChecker.getOrPut(branch.branchName) {
            try {
                DependencyVersionChecker(versionsRepository.getInitialVersion(libraryType, branch.toVersionClassifier()), "JDA") {
                    "https://raw.githubusercontent.com/${branch.ownerName}/${branch.repoName}/${branch.branchName}/pom.xml"
                }
            } catch (e: IOException) {
                throw RuntimeException("Unable to create branch specific JDA version checker", e)
            }
        }
        checkGithubBranchUpdates(branch, jdaVersionChecker)

        return jdaVersionChecker.latest.artifactInfo
    }

    private suspend fun checkGithubBranchUpdates(branch: GithubBranch, checker: VersionChecker) {
        val updateCountdown = updateCountdownMap.getOrPut(branch.branchName) { UpdateCountdown(1.minutes) }
        if (updateCountdown.needsUpdate()) {
            checker.checkVersion()
            context.invalidateAutocompleteCache(SlashJitpack.PR_NUMBER_AUTOCOMPLETE_NAME)
            checker.save(versionsRepository)
        }
    }

    private fun GithubBranch.toVersionClassifier() = "$ownerName-$repoName-$branchName"

    private fun retrieveBranchList(libraryType: LibraryType): GithubBranchMap {
        val map: Map<String, GithubBranch> = GithubUtils.getBranches(libraryType.githubOwnerName, libraryType.githubRepoName).associateBy { it.branchName }
        val defaultBranchName = GithubUtils.getDefaultBranchName(libraryType.githubOwnerName, libraryType.githubRepoName)
        val defaultBranch = map[defaultBranchName]!!
        return GithubBranchMap(defaultBranch, map)
    }
}
