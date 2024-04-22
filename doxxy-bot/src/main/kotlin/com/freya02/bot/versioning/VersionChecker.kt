package com.freya02.bot.versioning

abstract class VersionChecker protected constructor(latest: LibraryVersion) {
    val classifier: String? = latest.classifier
    var latest: ArtifactInfo = latest.artifactInfo
        private set
    private var diskLatest: ArtifactInfo = this.latest

    protected abstract fun retrieveLatest(): ArtifactInfo

    fun checkVersion(): Boolean {
        val latest = retrieveLatest()
        val changed = latest != diskLatest

        this.latest = latest
        return changed
    }
}