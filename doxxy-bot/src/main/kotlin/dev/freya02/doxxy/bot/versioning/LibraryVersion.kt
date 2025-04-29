package dev.freya02.doxxy.bot.versioning

data class LibraryVersion(
    val classifier: String?,
    val groupId: String,
    val artifactId: String,
    val version: String,
    var sourceUrl: String? = null //TODO move to LibraryVersionMetadata, setMetadata(LibraryType, String, ...)
)

val LibraryVersion.artifactInfo get() = ArtifactInfo(groupId, artifactId, version)