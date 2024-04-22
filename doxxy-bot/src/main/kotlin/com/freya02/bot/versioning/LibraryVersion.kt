package com.freya02.bot.versioning

data class LibraryVersion(
    val classifier: String?,
    val artifactInfo: ArtifactInfo,
    val sourceUrl: String?
)