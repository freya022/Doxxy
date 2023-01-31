package com.freya02.bot.versioning.jitpack

import com.freya02.bot.Data
import com.freya02.bot.commands.slash.versioning.SlashJitpack
import com.freya02.bot.versioning.ArtifactInfo
import com.freya02.bot.versioning.LibraryType
import com.freya02.bot.versioning.github.GithubBranch
import com.freya02.bot.versioning.github.GithubBranchMap
import com.freya02.bot.versioning.github.GithubUtils
import com.freya02.bot.versioning.github.UpdateCountdown
import com.freya02.bot.versioning.maven.MavenBranchProjectDependencyVersionChecker
import com.freya02.botcommands.api.BContext
import java.io.IOException
import java.util.*
import kotlin.time.Duration.Companion.minutes

class JitpackBranchService(private val context: BContext) {
    private val updateMap: MutableMap<LibraryType, UpdateCountdown> = EnumMap(LibraryType::class.java)
    private val branchMap: MutableMap<LibraryType, GithubBranchMap> = EnumMap(LibraryType::class.java)

    private val branchNameToJdaVersionChecker: MutableMap<String, MavenBranchProjectDependencyVersionChecker> =
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

    fun getUsedJDAVersionFromBranch(branch: GithubBranch): ArtifactInfo {
        val jdaVersionChecker = branchNameToJdaVersionChecker.getOrPut(branch.branchName) {
            try {
                return@getOrPut MavenBranchProjectDependencyVersionChecker(
                    Data.getBranchFileName(branch),
                    branch.ownerName,
                    branch.repoName,
                    "JDA",
                    branch.branchName
                )
            } catch (e: IOException) {
                throw RuntimeException("Unable to create branch specific JDA version checker", e)
            }
        }
        checkGithubBranchUpdates(branch, jdaVersionChecker)

        return jdaVersionChecker.latest
    }

    private fun checkGithubBranchUpdates(
        branch: GithubBranch,
        checker: MavenBranchProjectDependencyVersionChecker
    ) {
        val updateCountdown = updateCountdownMap.getOrPut(branch.branchName) { UpdateCountdown(1.minutes) }
        if (updateCountdown.needsUpdate()) {
            checker.checkVersion()
            context.invalidateAutocompleteCache(SlashJitpack.PR_NUMBER_AUTOCOMPLETE_NAME)
            checker.saveVersion()
        }
    }

    private fun retrieveBranchList(libraryType: LibraryType): GithubBranchMap {
        val (ownerName: String, repoName: String) = when (libraryType) {
            LibraryType.JDA -> arrayOf("DV8FromTheWorld", "JDA")
            LibraryType.BOT_COMMANDS -> arrayOf("freya022", "BotCommands")
            LibraryType.JDA_KTX -> arrayOf("MinnDevelopment", "jda-ktx")
            LibraryType.LAVA_PLAYER -> arrayOf("Walkyst", "lavaplayer-fork")
        }

        val map: Map<String, GithubBranch> = GithubUtils.getBranches(ownerName, repoName).associateBy { it.branchName }
        val defaultBranchName = GithubUtils.getDefaultBranchName(ownerName, repoName)
        val defaultBranch = map[defaultBranchName]!!
        return GithubBranchMap(defaultBranch, map)
    }
}
