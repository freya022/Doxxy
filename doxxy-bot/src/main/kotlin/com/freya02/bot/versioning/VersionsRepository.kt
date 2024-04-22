package com.freya02.bot.versioning

import io.github.freya022.botcommands.api.core.db.Database
import io.github.freya022.botcommands.api.core.db.preparedStatement
import io.github.freya022.botcommands.api.core.service.annotations.BService

@BService
class VersionsRepository(private val database: Database) {
    suspend fun findByName(versionType: VersionType): LibraryVersion? {
        return database.preparedStatement(
            sql = """
                SELECT group_id, artifact_id, version, source_url
                FROM library_version
                WHERE group_id = ?
                  AND artifact_id = ?
            """.trimIndent(),
            readOnly = true
        ) {
            executeQuery(versionType.libraryType.mavenGroupId, versionType.libraryType.mavenArtifactId).readOrNull()?.let {
                LibraryVersion(
                    ArtifactInfo(it["group_id"], it["artifact_id"], it["version"]),
                    it.getOrNull("source_url"),
                )
            }
        }
    }

    suspend fun save(libraryVersion: LibraryVersion) {
        database.preparedStatement("""
            INSERT INTO library_version (group_id, artifact_id, version, source_url)
            VALUES (?, ?, ?, ?)
            ON CONFLICT ON CONSTRAINT library_version_pkey DO UPDATE SET version    = EXCLUDED.version,
                                                                         source_url = EXCLUDED.source_url
        """.trimIndent()) {
            val (groupId, artifactId, version) = libraryVersion.artifactInfo
            executeUpdate(groupId, artifactId, version, libraryVersion.sourceUrl)
        }
    }
}