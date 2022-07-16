package com.freya02.bot.docs.index

import com.freya02.bot.db.DBAction
import com.freya02.bot.db.Database
import com.freya02.bot.docs.cached.CachedClass
import com.freya02.bot.docs.cached.CachedField
import com.freya02.bot.docs.cached.CachedMethod
import com.freya02.botcommands.api.Logging
import com.freya02.docs.DocSourceType
import com.freya02.docs.DocsSession
import com.freya02.docs.PageCache
import com.freya02.docs.data.SeeAlso.SeeAlsoReference
import com.freya02.docs.data.TargetType
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.dv8tion.jda.api.entities.MessageEmbed
import org.intellij.lang.annotations.Language
import org.slf4j.Logger

private val LOGGER: Logger = Logging.getLogger()

//Initial construct just allows database access
// Further updates must be invoked by external methods such as version checkers
class DocIndex(private val sourceType: DocSourceType, private val database: Database) : IDocIndexKt {
    private val mutex = Mutex()

    fun getClassDoc(className: String): CachedClass? {
        val (docId, embed, sourceLink) = findDoc(DocType.CLASS, className) ?: return null
        val seeAlsoReferences: List<SeeAlsoReference> = findSeeAlsoReferences(docId)

        return CachedClass(embed, seeAlsoReferences, sourceLink)
    }

    override fun getMethodDoc(className: String, identifier: String): CachedMethod? {
        val (docId, embed, sourceLink) = findDoc(DocType.METHOD, className, identifier) ?: return null
        val seeAlsoReferences: List<SeeAlsoReference> = findSeeAlsoReferences(docId)

        return CachedMethod(embed, seeAlsoReferences, sourceLink)
    }

    override fun getFieldDoc(className: String, fieldName: String): CachedField? {
        val (docId, embed, sourceLink) = findDoc(DocType.FIELD, className, fieldName) ?: return null
        val seeAlsoReferences: List<SeeAlsoReference> = findSeeAlsoReferences(docId)

        return CachedField(embed, seeAlsoReferences, sourceLink)
    }

    override fun findAnyMethodSignatures(query: String?): Collection<String> = getAllSignatures(DocType.METHOD, query)

    override fun findMethodSignatures(className: String, query: String?): Collection<String> =
        findSignatures(className, query, DocType.METHOD)

    override fun findAnyFieldSignatures(query: String?): Collection<String> = getAllSignatures(DocType.FIELD, query)

    override fun findFieldSignatures(className: String, query: String?): Collection<String> =
        findSignatures(className, query, DocType.FIELD)

    override fun findMethodAndFieldSignatures(className: String, query: String?): Collection<String> =
        findSignatures(className, query, DocType.METHOD, DocType.FIELD)

    override fun getClasses(query: String?): Collection<String> = runBlocking {
        @Language("PostgreSQL", prefix = "select * from doc ")
        val limitingSort = when (query) {
            null -> ""
            else -> "order by similarity(classname, ?) desc limit 25"
        }

        val sortArgs = when (query) {
            null -> arrayOf()
            else -> arrayOf(query)
        }

        database.preparedStatement(
            """
            select classname
            from doc
            where source_id = ?
              and type = ?
            $limitingSort
            """.trimIndent()
        ) {
            executeQuery(sourceType.id, DocType.CLASS.id, *sortArgs).transformEach { it.getString("classname") }
        }
    }

    override fun getClassesWithMethods(query: String?): Collection<String> =
        getClassNamesWithChildren(DocType.METHOD, query)

    override fun getClassesWithFields(query: String?): Collection<String> =
        getClassNamesWithChildren(DocType.FIELD, query)

    suspend fun reindex(): DocIndex {
        mutex.withLock {
            LOGGER.info("Re-indexing docs for {}", sourceType.name)

            if (sourceType != DocSourceType.JAVA) {
                LOGGER.info("Clearing cache for {}", sourceType.name)
                PageCache[sourceType].clearCache()
                LOGGER.info("Cleared cache of {}", sourceType.name)
            }

            val docsSession = DocsSession()

            DocIndexWriter(database, docsSession, sourceType).doReindex()

            LOGGER.info("Re-indexed docs for {}", sourceType.name)
        }

        System.gc() //Very effective

        return this
    }

    private fun findDoc(
        docType: DocType,
        className: String,
        identifier: String? = null
    ): Triple<Int, MessageEmbed, String>? = DBAction.of(
        database,
        """
            select id, embed, source_link
            from doc
            where source_id = ?
              and type = ?
              and classname = ?
              and quote_nullable(identifier) = quote_nullable(?)
            limit 1""".trimIndent(),
        "id", "embed"
    ).use {
        val result = it.executeQuery(sourceType.id, docType.id, className, identifier).readOnce() ?: return null
        return@use Triple(
            result.getInt("id"),
            DocIndexWriter.GSON.fromJson(result.getString("embed"), MessageEmbed::class.java),
            result["source_link"]
        )
    }

    private fun findSeeAlsoReferences(docId: Int): List<SeeAlsoReference> = DBAction.of(
        database,
        """
            select text, link, target_type, full_signature
            from docseealsoreference
            where doc_id = ?""".trimIndent(),
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

    private fun getAllSignatures(docType: DocType, query: String?): List<String> {
        @Language("PostgreSQL", prefix = "select * from doc ")
        val sort = when {
            query == null || query.isEmpty() -> "order by classname, identifier"
            '#' in query -> "order by similarity(classname, ?) * similarity(left(identifier, strpos(identifier, '(')), ?) desc"
            else -> "order by similarity(concat(classname, '#', left(identifier, strpos(identifier, '('))), ?) desc"
        }

        val sortArgs = when {
            query == null || query.isEmpty() -> arrayOf()
            '#' in query -> arrayOf(query.substringBefore('#'), query.substringAfter('#'))
            else -> arrayOf(query)
        }

        return DBAction.of(
            database,
            """
                select classname, identifier
                from doc
                where source_id = ?
                  and type = ?
                $sort
                limit 25
                """.trimIndent(),
            "classname"
        ).use { action ->
            action.executeQuery(sourceType.id, docType.id, *sortArgs)
                .transformEach { "${it.get<String>("classname")}#${it.get<String>("identifier")}" }
        }
    }

    private fun findSignatures(className: String, query: String?, vararg docTypes: DocType): List<String> {
        val typeCheck = docTypes.joinToString(" or ") { "doc.type = ${it.id}" }

        @Language("PostgreSQL", prefix = "select * from doc ")
        val sort = when {
            query == null || query.isEmpty() -> "order by identifier"
            else -> "order by similarity(left(identifier, strpos(identifier, '(')), ?) desc"
        }

        val sortArgs = when {
            query == null || query.isEmpty() -> arrayOf()
            else -> arrayOf(query)
        }

        return DBAction.of(
            database,
            """
                select identifier
                from doc
                where source_id = ?
                  and ($typeCheck)
                  and classname = ?
                $sort
                limit 25
                """.trimIndent(),
            "identifier"
        ).use { action ->
            action.executeQuery(sourceType.id, className, *sortArgs)
                .transformEach { it.getString("identifier") }
        }
    }

    private fun getClassNamesWithChildren(docType: DocType, query: String?): List<String> {
        @Language("PostgreSQL", prefix = "select * from doc ")
        val sort = when {
            query == null || query.isEmpty() -> "order by classname"
            else -> "order by similarity(classname, ?) desc"
        }

        val sortArgs = when {
            query == null || query.isEmpty() -> arrayOf()
            else -> arrayOf(query)
        }

        return DBAction.of(
            database,
            """
                select classname
                from doc
                where source_id = ?
                  and type = ?
                group by classname
                $sort
                limit 25
                """.trimIndent(),
            "classname"
        ).use { action ->
            action.executeQuery(sourceType.id, docType.id, *sortArgs).transformEach { it.getString("classname") }
        }
    }
}