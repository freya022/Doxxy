package dev.freya02.doxxy.bot.docs

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNamingStrategy
import kotlinx.serialization.json.decodeFromStream
import kotlin.io.path.Path
import kotlin.io.path.inputStream

@Serializable
private data class Doc(
    val sourceId: Int,
    val classname: String,
    val identifier: String?,
    val embed: String,
)

private val Doc.fullIdent get() = if (identifier == null)
    classname
else
    "$classname#$identifier"

// Export the data in a JSON from the two databases to compare
// in doc_local.json and doc_prod.json
@OptIn(ExperimentalSerializationApi::class)
fun main() {
    val json = Json {
        ignoreUnknownKeys = true
        namingStrategy = JsonNamingStrategy.SnakeCase
    }

    val sourceId = DocSourceType.JDA.id
    val localDocs = Path("doc_local.json").inputStream()
        .use { json.decodeFromStream<List<Doc>>(it) }
        .filter { it.sourceId == sourceId }
    val prodDocs = Path("doc_prod.json").inputStream()
        .use { json.decodeFromStream<List<Doc>>(it) }
        .filter { it.sourceId == sourceId }

    val localDocsMap = localDocs.associateBy { it.fullIdent }
    val prodDocsMap = prodDocs.associateBy { it.fullIdent }

    val localFullIdent = localDocsMap.keys
    val prodFullIdent = prodDocsMap.keys

    val removedIdents = prodFullIdent - localFullIdent
    val addedIdents = localFullIdent - prodFullIdent

    val embedDiffs = localDocs
        // Intersect local and prod Javadocs by their full identifier
        .filterNot { it.fullIdent in removedIdents || it.fullIdent in addedIdents }
        // Keep Javadocs with differing embeds
        .filter { local -> local.embed != prodDocsMap[local.fullIdent]!!.embed }
        .map { it.fullIdent }
        // Associate full identifier to local and prod Javadocs
        .associateWith { localDocsMap[it]!!.embed to prodDocsMap[it]!!.embed }

    println("Number of added identifiers: $addedIdents")
    println("Number of removed identifiers: $removedIdents")
    println("Number of different embeds: $embedDiffs")
}