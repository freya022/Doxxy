package com.freya02.bot.versioning

abstract class VersionChecker protected constructor(latest: ArtifactInfo) {
    var latest: ArtifactInfo = latest
        private set
    private var diskLatest: ArtifactInfo = latest

    protected abstract fun retrieveLatest(): ArtifactInfo

    fun checkVersion(): Boolean {
        val latest = retrieveLatest()
        val changed = latest != diskLatest

        this.latest = latest
        return changed
    }
}