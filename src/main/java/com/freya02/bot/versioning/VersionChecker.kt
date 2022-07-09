package com.freya02.bot.versioning

import com.freya02.bot.versioning.ArtifactInfo.Companion.fromFileString
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path

abstract class VersionChecker protected constructor(private val lastSavedPath: Path) {
    var latest: ArtifactInfo
        protected set
    protected var diskLatest: ArtifactInfo

    init {
        diskLatest = fromFileString(lastSavedPath)
        latest = diskLatest
    }

    @Throws(IOException::class)
    abstract fun checkVersion(): Boolean

    @Throws(IOException::class)
    fun saveVersion() {
        Files.writeString(lastSavedPath, latest.toFileString())

        diskLatest = latest
    }
}