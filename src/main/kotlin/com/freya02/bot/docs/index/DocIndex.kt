package com.freya02.bot.docs.index

import com.freya02.bot.db.DBAction
import com.freya02.bot.db.Database
import com.freya02.bot.docs.cached.CachedClass
import com.freya02.bot.docs.cached.CachedDoc
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
class DocIndex(private val sourceType: DocSourceType, private val database: Database) : IDocIndex {
    private val mutex = Mutex()

    override fun getClassDoc(className: String): CachedClass? {
        val (docId, embed, javadocLink, sourceLink) = findDoc(DocType.CLASS, className) ?: return null
        val seeAlsoReferences: List<SeeAlsoReference> = findSeeAlsoReferences(docId)

        return CachedClass(embed, seeAlsoReferences, javadocLink, sourceLink)
    }

    override fun getMethodDoc(className: String, identifier: String): CachedMethod? {
        val (docId, embed, javadocLink, sourceLink) = findDoc(DocType.METHOD, className, identifier) ?: return null
        val seeAlsoReferences: List<SeeAlsoReference> = findSeeAlsoReferences(docId)

        return CachedMethod(embed, seeAlsoReferences, javadocLink, sourceLink)
    }

    override fun getFieldDoc(className: String, fieldName: String): CachedField? {
        val (docId, embed, javadocLink, sourceLink) = findDoc(DocType.FIELD, className, fieldName) ?: return null
        val seeAlsoReferences: List<SeeAlsoReference> = findSeeAlsoReferences(docId)

        return CachedField(embed, seeAlsoReferences, javadocLink, sourceLink)
    }

    override fun findAnySignatures(docType: DocType, query: String?): List<DocSearchResult> = getAllSignatures(docType, query)

    override fun findSignaturesIn(className: String, query: String?, vararg docTypes: DocType): List<DocSearchResult> {
        val typeCheck = docTypes.joinToString(" or ") { "doc.type = ${it.id}" }

        @Language("PostgreSQL", prefix = "select * from doc ")
        val sort = when {
            query.isNullOrEmpty() -> "order by identifier"
            else -> "order by similarity(identifier_no_args, ?) desc"
        }

        val sortArgs = when {
            query.isNullOrEmpty() -> arrayOf()
            else -> arrayOf(query)
        }

        return DBAction.of(
            database,
            """
                select identifier, human_identifier, human_class_identifier
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
                .transformEach {
                    DocSearchResult(
                        it["identifier"],
                        it["human_identifier"],
                        it["human_class_identifier"],
                    )
                }
        }
    }

    override fun getClasses(query: String?): List<String> = runBlocking {
        @Language("PostgreSQL", prefix = "select * from doc ")
        val limitingSort = when {
            query.isNullOrEmpty() -> "order by classname limit 25"
            else -> "order by similarity(classname, ?) desc limit 25"
        }

        val sortArgs = when {
            query.isNullOrEmpty() -> arrayOf()
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
            executeQuery(sourceType.id, DocType.CLASS.id, *sortArgs).transformEach { it["classname"] }
        }
    }

    override fun getClassesWithMethods(query: String?): List<String> =
        getClassNamesWithChildren(DocType.METHOD, query)

    override fun getClassesWithFields(query: String?): List<String> =
        getClassNamesWithChildren(DocType.FIELD, query)

    override fun resolveDoc(query: String): CachedDoc? = runBlocking {
        // TextChannel#getIterableHistory()
        val tokens = query.split('#').toMutableList()
        var currentClass: String = tokens.removeFirst()
        var docsOf: String? = currentClass

        database.transactional {
            tokens.forEach {
                if (it.isEmpty()) {
                    // TextChannel#getIterableHistory()#
                    //  This means the last return type's docs must be sent
                    docsOf = currentClass
                    return@transactional
                }

                preparedStatement("select return_type from doc where source_id = ? and lower(classname) = lower(?) and lower(identifier) = lower(?)") {
                    val result = executeQuery(sourceType.id, currentClass, it).readOnce() ?: return@runBlocking null

                    docsOf = "$currentClass#$it"
                    currentClass = result["return_type"]
                }
            }
        }

        return@runBlocking docsOf?.let { docsOf ->
            when {
                '(' in docsOf -> getMethodDoc(docsOf)
                '#' in docsOf -> getFieldDoc(docsOf)
                else -> getClassDoc(docsOf)
            }
        }
    }

    override fun resolveDocAutocomplete(query: String): List<DocResolveResult> = runBlocking {
        // TextChannel#getIter ==> TextChannel#getIterableHistory()
        // TextChannel#getIterableHistory()#forEachAsy ==> MessagePaginationAction#forEachAsync()
        val tokens = query.split('#').toMutableList()
        var currentClass: String = tokens.removeFirst()
        val lastToken = tokens.lastOrNull()

        //Get class name of the last returned object
        database.transactional {
            tokens.dropLast(1).forEach {
                preparedStatement("select return_type from doc where source_id = ? and classname = ? and identifier = ?") {
                    val result = executeQuery(sourceType.id, currentClass, it).readOnce() ?: return@runBlocking emptyList()

                    currentClass = result["return_type"]
                }
            }
        }

        //This happens with "TextChannel#getIterableHistory()#", this gets the docs of the return type of getIterableHistory
        if (lastToken?.isEmpty() == true) {
            return@runBlocking listOf(DocResolveResult(currentClass, currentClass))
        }

        //Do a classic search on the latest return type + optionally last token (might be a method or a field)
        return@runBlocking when {
            lastToken != null -> {
                findSignaturesIn(currentClass, lastToken, DocType.METHOD, DocType.FIELD)
                    //Current class is added because findSignaturesIn doesn't return "identifier", not "full_signature"
                    .map { DocResolveResult(it.humanClassIdentifier, "$currentClass#${it.identifierOrFullIdentifier}") }
            }
            else -> getClasses(currentClass).map { DocResolveResult(it, it) }
        }
    }

    suspend fun reindex(reindexData: ReindexData): DocIndex {
        mutex.withLock {
            LOGGER.info("Re-indexing docs for {}", sourceType.name)

            if (sourceType != DocSourceType.JAVA) {
                LOGGER.info("Clearing cache for {}", sourceType.name)
                PageCache[sourceType].clearCache()
                LOGGER.info("Cleared cache of {}", sourceType.name)
            }

            val docsSession = DocsSession()

            DocIndexWriter(database, docsSession, sourceType, reindexData).doReindex()

            LOGGER.info("Re-indexed docs for {}", sourceType.name)
        }

        System.gc() //Very effective

        return this
    }

    private fun findDoc(
        docType: DocType,
        className: String,
        identifier: String? = null
    ): DocFindData? = DBAction.of(
        database,
        """
            select id, embed, javadoc_link, source_link
            from doc
            where source_id = ?
              and type = ?
              and classname = ?
              and quote_nullable(identifier) = quote_nullable(?)
            limit 1""".trimIndent(),
        "id", "embed"
    ).use {
        val result = it.executeQuery(sourceType.id, docType.id, className, identifier).readOnce() ?: return null
        return@use DocFindData(
            result["id"],
            DocIndexWriter.GSON.fromJson(result.getString("embed"), MessageEmbed::class.java),
            result["javadoc_link"],
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

    private fun getAllSignatures(docType: DocType, query: String?): List<DocSearchResult> {
        @Language("PostgreSQL", prefix = "select * from doc ")
        val sort = when {
            query.isNullOrEmpty() -> "order by classname, identifier"
            '#' in query -> "order by similarity(classname, ?) * similarity(identifier_no_args, ?) desc"
            else -> "order by similarity(concat(classname, '#', identifier_no_args), ?) desc"
        }

        val sortArgs = when {
            query.isNullOrEmpty() -> arrayOf()
            '#' in query -> arrayOf(query.substringBefore('#'), query.substringAfter('#').substringBefore('('))
            else -> arrayOf(query)
        }

        return DBAction.of(
            database,
            """
                select concat(classname, '#', identifier) as full_identifier, human_identifier, human_class_identifier
                from doc
                where source_id = ?
                  and type = ?
                $sort
                limit 25
                """.trimIndent(),
            "classname"
        ).use { action ->
            action.executeQuery(sourceType.id, docType.id, *sortArgs)
                .transformEach {
                    DocSearchResult(
                        it["full_identifier"],
                        it["human_identifier"],
                        it["human_class_identifier"]
                    )
                }
        }
    }

    private fun getClassNamesWithChildren(docType: DocType, query: String?): List<String> {
        @Language("PostgreSQL", prefix = "select * from doc ")
        val sort = when {
            query.isNullOrEmpty() -> "order by classname"
            else -> "order by similarity(classname, ?) desc"
        }

        val sortArgs = when {
            query.isNullOrEmpty() -> arrayOf()
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