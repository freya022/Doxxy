package com.freya02.bot.versioning.jitpack

import com.freya02.bot.versioning.LibraryType
import com.freya02.bot.versioning.github.GithubBranch
import com.freya02.bot.versioning.github.GithubBranchMap
import com.freya02.bot.versioning.github.GithubUtils
import com.freya02.bot.versioning.github.UpdateCountdown
import java.util.*
import kotlin.time.Duration.Companion.minutes

class JitpackBranchService {
    private val updateMap: MutableMap<LibraryType, UpdateCountdown> = EnumMap(LibraryType::class.java)
    private val branchMap: MutableMap<LibraryType, GithubBranchMap> = EnumMap(LibraryType::class.java)

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

    private fun retrieveBranchList(libraryType: LibraryType): GithubBranchMap {
        val (ownerName: String, repoName: String) = when (libraryType) {
            LibraryType.JDA5 -> arrayOf("DV8FromTheWorld", "JDA")
            LibraryType.BOT_COMMANDS -> arrayOf("freya022", "BotCommands")
            LibraryType.JDA_KTX -> arrayOf("MinnDevelopment", "jda-ktx")
            else -> throw IllegalArgumentException("No branches for $libraryType")
        }

        val map: Map<String, GithubBranch> = GithubUtils.getBranches(ownerName, repoName).associateBy { it.branchName }
        val defaultBranchName = GithubUtils.getDefaultBranchName(ownerName, repoName)
        val defaultBranch = map[defaultBranchName]!!
        return GithubBranchMap(defaultBranch, map)
    }

    fun getBranch(libraryType: LibraryType, branchName: String?): GithubBranch? {
        val githubBranchMap = getBranchMap(libraryType)
        return when (branchName) {
            null -> githubBranchMap.defaultBranch
            else -> githubBranchMap.branches[branchName]
        }
    }
}