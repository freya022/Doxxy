package com.freya02.bot.versioning

import io.github.freya022.botcommands.api.core.db.Database
import io.github.freya022.botcommands.api.core.db.preparedStatement
import io.github.freya022.botcommands.api.core.service.annotations.BService
import kotlinx.coroutines.runBlocking

@BService
class VersionsRepository(private val database: Database) {
    suspend fun findByName(libraryType: LibraryType, classifier: String?): LibraryVersion? {
        return database.preparedStatement(
            sql = """
                SELECT group_id, artifact_id, classifier, version, source_url
                FROM library_version
                WHERE group_id = ?
                  AND artifact_id = ?
                  AND classifier = ?
            """.trimIndent(),
            readOnly = true
        ) {
            executeQuery(libraryType.mavenGroupId, libraryType.mavenArtifactId, classifier).readOrNull()?.let {
                LibraryVersion(
                    it.getOrNull("classifier"),
                    ArtifactInfo(it["group_id"], it["artifact_id"], it["version"]),
                    it.getOrNull("source_url"),
                )
            }
        }
    }

    suspend fun save(libraryVersion: LibraryVersion) {
        database.preparedStatement("""
            INSERT INTO library_version (group_id, artifact_id, classifier, version, source_url)
            VALUES (?, ?, ?, ?, ?)
            ON CONFLICT ON CONSTRAINT library_version_pkey DO UPDATE SET version    = EXCLUDED.version,
                                                                         source_url = EXCLUDED.source_url
        """.trimIndent()) {
            val (groupId, artifactId, version) = libraryVersion.artifactInfo
            executeUpdate(groupId, artifactId, libraryVersion.classifier, version, libraryVersion.sourceUrl)
        }
    }
}

fun VersionsRepository.getInitialVersion(libraryType: LibraryType, classifier: String? = null): ArtifactInfo = runBlocking {
    findByName(libraryType, classifier)
        ?.artifactInfo
        ?: ArtifactInfo.emptyVersion(libraryType.mavenGroupId, libraryType.mavenArtifactId)
}