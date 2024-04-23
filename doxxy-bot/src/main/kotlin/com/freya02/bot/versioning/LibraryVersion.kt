package com.freya02.bot.versioning

data class LibraryVersion(
    val classifier: String?,
    val groupId: String,
    val artifactId: String,
    var version: String,
    var sourceUrl: String? = null
)

val LibraryVersion.artifactInfo get() = ArtifactInfo(groupId, artifactId, version)