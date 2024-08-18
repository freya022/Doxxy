package com.freya02.bot.versioning

abstract class VersionChecker protected constructor(latest: LibraryVersion) {
    var latest: LibraryVersion = latest
        private set

    private var savedVersion: LibraryVersion = latest

    protected abstract fun retrieveLatest(): LibraryVersion

    fun checkVersion(): Boolean {
        val latestVersion = retrieveLatest()
        val changed = latestVersion != latest

        this.latest = latestVersion
        return changed
    }

    suspend fun save(versionsRepository: VersionsRepository, sourceUrl: String? = null) {
        latest.sourceUrl = sourceUrl
        versionsRepository.save(latest)
        savedVersion = latest
    }
}