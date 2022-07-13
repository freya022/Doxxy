package com.freya02.bot.docs.index

import com.freya02.bot.db.DBAction
import com.freya02.bot.db.Database
import com.freya02.bot.docs.cached.CachedClass
import com.freya02.bot.docs.cached.CachedField
import com.freya02.bot.docs.cached.CachedMethod
import com.freya02.botcommands.api.Logging
import com.freya02.docs.DocSourceType
import com.freya02.docs.DocsSession
import com.freya02.docs.data.SeeAlso.SeeAlsoReference
import com.freya02.docs.data.TargetType
import com.freya02.docs2.PageCache
import net.dv8tion.jda.api.entities.MessageEmbed
import org.slf4j.Logger

private val LOGGER: Logger = Logging.getLogger()

//Initial construct just allows database access
// Further updates must be invoked by external methods such as version checkers
class DocIndexKt(private val sourceType: DocSourceType, private val database: Database) : IDocIndexKt {
    fun getClassDoc(className: String): CachedClass? {
        val (docId, embed) = findDoc(DocType.CLASS, className) ?: return null
        val seeAlsoReferences: List<SeeAlsoReference> = findSeeAlsoReferences(docId)

        return CachedClass(embed, seeAlsoReferences)
    }

    override fun getMethodDoc(className: String, identifier: String): CachedMethod? {
        val (docId, embed) = findDoc(DocType.METHOD, "$className#$identifier") ?: return null
        val seeAlsoReferences: List<SeeAlsoReference> = findSeeAlsoReferences(docId)

        return CachedMethod(embed, seeAlsoReferences)
    }

    override fun getFieldDoc(className: String, fieldName: String): CachedField? {
        val (docId, embed) = findDoc(DocType.FIELD, "$className#$fieldName") ?: return null
        val seeAlsoReferences: List<SeeAlsoReference> = findSeeAlsoReferences(docId)

        return CachedField(embed, seeAlsoReferences)
    }

    override fun getAllMethodSignatures(): Collection<String> =
        getAllSignatures(DocType.METHOD)

    override fun findMethodSignatures(className: String): Collection<String> =
        findSignatures(DocType.METHOD, className)

    override fun getAllFieldSignatures(): Collection<String> {
        TODO("Not yet implemented")
    }

    override fun findFieldSignatures(className: String): Collection<String> {
        TODO("Not yet implemented")
    }

    override fun getMethodAndFieldSignatures(className: String): Collection<String> {
        TODO("Not yet implemented")
    }

    override fun getSimpleNameList(): Collection<String> {
        TODO("Not yet implemented")
    }

    override fun getClassesWithMethods(): Collection<String> {
        TODO("Not yet implemented")
    }

    override fun getClassesWithFields(): Collection<String> {
        TODO("Not yet implemented")
    }

    fun reindex(): DocIndexKt {
        LOGGER.info("Re-indexing docs for {}", sourceType.name)

        LOGGER.info("Clearing cache for {}", sourceType.name)
        PageCache.clearCache(sourceType)
        LOGGER.info("Cleared cache of {}", sourceType.name)

        val docsSession = DocsSession()

        DocIndexWriter(database, docsSession, sourceType).doReindex()

        LOGGER.info("Re-indexed docs for {}", sourceType.name)

        System.gc() //Very effective

        return this
    }

    private fun findSeeAlsoReferences(docId: Int): List<SeeAlsoReference> = DBAction.of(
        database,
        "select text, link, target_type, full_signature from docseealsoreference where doc_id = ?",
        "text", "link", "target_type", "full_signature"
    ).use { action ->
        action.executeQuery(docId).transformEach {
            SeeAlsoReference(
                it.getString("text"),
                it.getString("link"),
                TargetType.fromId(it.getInt("target_type")),
                it.getString("full_signature")
            )
        }
    }

    private fun findDoc(docType: DocType, className: String): Pair<Int, MessageEmbed>? = DBAction.of(
        database,
        "select id, embed from doc where type = ? and name = ? limit 1",
        "id", "embed"
    ).use {
        val result = it.executeQuery(docType.id, className).readOnce() ?: return null
        return@use result.getInt("id") to DocIndexWriter.GSON.fromJson(
            result.getString("embed"),
            MessageEmbed::class.java
        )
    }

    private fun getAllSignatures(docType: DocType): List<String> =
        DBAction.of(
            database,
            "select doc.name from doc where type = ?",
            "name"
        ).use { action ->
            action.executeQuery(docType.id).transformEach { it.getString("name") }
        }

    private fun findSignatures(docType: DocType, className: String): List<String> =
        DBAction.of(
            database,
            "select doc.name from doc join doc parentDoc on doc.parent_id = parentDoc.id where doc.type = ? and parentDoc.name = ?",
            "name"
        ).use { action ->
            action.executeQuery(docType.id, className).transformEach { it.getString("name") }
        }
}