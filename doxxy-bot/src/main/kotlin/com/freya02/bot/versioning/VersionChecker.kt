package com.freya02.bot.versioning

abstract class VersionChecker protected constructor(latest: LibraryVersion) {
    var latest: LibraryVersion = latest
        private set

    private var latestVersion: String = latest.artifactInfo.version
    private var diskLatestVersion: String = latestVersion

    protected abstract fun retrieveLatest(): String

    fun checkVersion(): Boolean {
        val latestVersion = retrieveLatest()
        val changed = latestVersion != diskLatestVersion

        this.latest.version = latestVersion
        return changed
    }

    suspend fun save(versionsRepository: VersionsRepository, sourceUrl: String? = null) {
        latest.sourceUrl = sourceUrl
        versionsRepository.save(latest)
        diskLatestVersion = latestVersion
    }
}