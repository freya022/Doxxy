package com.freya02.bot.versioning.maven

import com.freya02.bot.versioning.ArtifactInfo
import com.freya02.bot.versioning.VersionChecker
import java.io.IOException
import java.nio.file.Path

class MavenVersionChecker(
    lastSavedPath: Path?,
    private val repoType: RepoType,
    private val groupId: String,
    private val artifactId: String
) : VersionChecker(
    lastSavedPath!!
) {
    @Throws(IOException::class)
    override fun checkVersion(): Boolean {
        ArtifactInfo(
            groupId,
            artifactId,
            MavenUtils.getLatestStableMavenVersion(repoType.urlFormat, groupId, artifactId)
        ).let { latestVersion ->
            val changed = latestVersion != diskLatest
            latest = latestVersion

            return changed
        }
    }
}