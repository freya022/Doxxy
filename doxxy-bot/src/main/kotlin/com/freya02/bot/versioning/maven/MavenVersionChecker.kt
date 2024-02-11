package com.freya02.bot.versioning.maven

import com.freya02.bot.versioning.ArtifactInfo
import com.freya02.bot.versioning.LibraryType
import com.freya02.bot.versioning.VersionChecker
import java.io.IOException
import java.nio.file.Path

class MavenVersionChecker(
    lastSavedPath: Path,
    private val libraryType: LibraryType
) : VersionChecker(lastSavedPath) {
    @Throws(IOException::class)
    override fun checkVersion(): Boolean {
        ArtifactInfo(
            libraryType.mavenGroupId,
            libraryType.mavenArtifactId,
            MavenUtils.getLatestStableMavenVersion(libraryType.repoType, libraryType.mavenGroupId, libraryType.mavenArtifactId)
        ).let { latestVersion ->
            val changed = latestVersion != diskLatest
            latest = latestVersion

            return changed
        }
    }
}