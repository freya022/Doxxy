package com.freya02.bot.versioning

import io.github.freya022.botcommands.api.core.db.Database
import io.github.freya022.botcommands.api.core.db.preparedStatement
import io.github.freya022.botcommands.api.core.service.annotations.BService
import kotlinx.coroutines.runBlocking

/**
 * Repository for library versions.
 *
 * Each row is composed of the Maven artifact coordinates,
 * a classifier is used to differentiate the rows which correspond to a version in a different context.
 *
 * For example, JDA's latest version has a group id and artifact id, but no classifier.
 * Another example is the JDA version used in BotCommands,
 * it has a group id, artifact id, and a classifier which tells that this version is from BC.
 */
@BService
class VersionsRepository(private val database: Database) {
    suspend fun findByName(libraryType: LibraryType, classifier: String?): LibraryVersion? {
        return database.preparedStatement(
            sql = """
                SELECT group_id, artifact_id, classifier, version, source_url
                FROM library_version
                WHERE group_id = ?
                  AND artifact_id = ?
                  AND classifier IS NOT DISTINCT FROM ?
            """.trimIndent(),
            readOnly = true
        ) {
            executeQuery(libraryType.mavenGroupId, libraryType.mavenArtifactId, classifier).readOrNull()?.let {
                LibraryVersion(
                    it.getOrNull("classifier"),
                    it["group_id"], it["artifact_id"], it["version"],
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
            val (classifier, groupId, artifactId, version, sourceUrl) = libraryVersion
            executeUpdate(groupId, artifactId, classifier, version, sourceUrl)
        }
    }
}

fun VersionsRepository.getInitialVersion(libraryType: LibraryType, classifier: String? = null): LibraryVersion = runBlocking {
    findByName(libraryType, classifier)
        ?: LibraryVersion(classifier, libraryType.mavenGroupId, libraryType.mavenArtifactId, "Unknown")
}