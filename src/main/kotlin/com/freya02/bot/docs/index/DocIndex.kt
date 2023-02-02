package com.freya02.bot.docs.index

import com.freya02.bot.docs.cached.CachedClass
import com.freya02.bot.docs.cached.CachedDoc
import com.freya02.bot.docs.cached.CachedField
import com.freya02.bot.docs.cached.CachedMethod
import com.freya02.bot.utils.QueryUnion
import com.freya02.botcommands.api.core.db.Database
import com.freya02.docs.DocSourceType
import com.freya02.docs.DocsSession
import com.freya02.docs.PageCache
import com.freya02.docs.data.SeeAlso.SeeAlsoReference
import com.freya02.docs.data.TargetType
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import mu.KotlinLogging
import net.dv8tion.jda.api.entities.MessageEmbed
import org.intellij.lang.annotations.Language

//Initial construct just allows database access
// Further updates must be invoked by external methods such as version checkers
class DocIndex(val sourceType: DocSourceType, private val database: Database) : IDocIndex {
    private val mutex = Mutex()

    override suspend fun getClassDoc(className: String): CachedClass? {
        val (docId, embed, javadocLink, sourceLink) = findDoc(DocType.CLASS, className) ?: return null
        val seeAlsoReferences: List<SeeAlsoReference> = findSeeAlsoReferences(docId)

        return CachedClass(sourceType, embed, seeAlsoReferences, javadocLink, sourceLink)
    }

    override suspend fun getMethodDoc(className: String, identifier: String): CachedMethod? {
        val (docId, embed, javadocLink, sourceLink) = findDoc(DocType.METHOD, className, identifier) ?: return null
        val seeAlsoReferences: List<SeeAlsoReference> = findSeeAlsoReferences(docId)

        return CachedMethod(sourceType, embed, seeAlsoReferences, javadocLink, sourceLink)
    }

    override suspend fun getFieldDoc(className: String, fieldName: String): CachedField? {
        val (docId, embed, javadocLink, sourceLink) = findDoc(DocType.FIELD, className, fieldName) ?: return null
        val seeAlsoReferences: List<SeeAlsoReference> = findSeeAlsoReferences(docId)

        return CachedField(sourceType, embed, seeAlsoReferences, javadocLink, sourceLink)
    }

    override suspend fun findAnySignatures(query: String, limit: Int, docTypes: DocTypes): List<DocSearchResult> {
        if (docTypes.isEmpty()) throw IllegalArgumentException("Must have at least one doc type")
        @Language("PostgreSQL", prefix = "select ", suffix = " from doc natural left join doc_view")
        val similarityScoreQuery = when {
            '#' in query -> "similarity(?, classname) * similarity(?, identifier_no_args)"
            else -> "similarity(?, coalesce(identifier_no_args, classname))"
        }
        val similarityScoreQueryParams = when {
            '#' in query -> arrayOf(query.substringBefore('#'), query.substringAfter('#'))
            else -> arrayOf(query)
        }

        return database.preparedStatement("""
            select coalesce(full_identifier, classname)        as full_identifier,
                   coalesce(human_identifier, classname)       as human_identifier,
                   coalesce(human_class_identifier, classname) as human_class_identifier,
                   $similarityScoreQuery                       as overall_similarity
            from doc
                     natural left join doc_view
            where source_id = ?
            order by overall_similarity desc, full_identifier
            limit ?;
        """.trimIndent()) {
            executeQuery(*similarityScoreQueryParams, sourceType.id, limit).map {
                DocSearchResult(
                    it["full_identifier"],
                    it["human_identifier"],
                    it["human_class_identifier"]
                )
            }
        }
    }

    override suspend fun findSignaturesIn(className: String, query: String?, docTypes: DocTypes, limit: Int): List<DocSearchResult> {
        @Language("PostgreSQL", prefix = "select * from doc ")
        val sort = when {
            query.isNullOrEmpty() -> "order by identifier"
            else -> "order by similarity(identifier_no_args, ?) desc"
        }

        val sortArgs = when {
            query.isNullOrEmpty() -> arrayOf()
            else -> arrayOf(query)
        }

        database.preparedStatement("""
                select identifier, human_identifier, human_class_identifier
                from doc
                where source_id = ?
                  and type = any (?)
                  and classname = ?
                $sort
                limit ?
                """.trimIndent()
        ) {
            return executeQuery(sourceType.id, docTypes.map { it.id }.toTypedArray(), className, *sortArgs, limit)
                .transformEach {
                    DocSearchResult(
                        it["identifier"],
                        it["human_identifier"],
                        it["human_class_identifier"],
                    )
                }
        }
    }

    override suspend fun getClasses(query: String?, limit: Int): List<String> {
        @Language("PostgreSQL", prefix = "select * from doc ")
        val limitingSort = when {
            query.isNullOrEmpty() -> "order by classname limit ?"
            else -> "order by similarity(classname, ?) desc limit ?"
        }

        val sortArgs = when {
            query.isNullOrEmpty() -> arrayOf(limit)
            else -> arrayOf(query, limit)
        }

        return database.preparedStatement(
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

    override suspend fun resolveDoc(query: String): CachedDoc? {
        // TextChannel#getIterableHistory()
        val tokens = query.split('#').toMutableList()
        var currentClass: String = tokens.removeFirst()
        var docsOf: String = currentClass

        database.transactional {
            tokens.forEach {
                if (it.isEmpty()) {
                    // TextChannel#getIterableHistory()#
                    //  This means the last return type's docs must be sent
                    docsOf = currentClass
                    return@transactional
                }

                preparedStatement("select return_type from doc where source_id = ? and lower(classname) = lower(?) and lower(identifier) = lower(?)") {
                    val result = executeQuery(sourceType.id, currentClass, it).readOnce() ?: return null

                    docsOf = "$currentClass#$it"
                    currentClass = result["return_type"]
                }
            }
        }

        return when {
            '(' in docsOf -> getMethodDoc(docsOf)
            '#' in docsOf -> getFieldDoc(docsOf)
            else -> getClassDoc(docsOf)
        }
    }

    //TODO make it resolve incrementally with each token, so that it picks the #1 result of the previous chain before resolving the next token
    override suspend fun resolveDocAutocomplete(query: String): List<DocResolveResult> {
        // TextChannel#getIter ==> TextChannel#getIterableHistory()
        // TextChannel#getIterableHistory()#forEachAsy ==> MessagePaginationAction#forEachAsync()
        val tokens = query.split('#').toMutableList()
        var currentClass: String = tokens.removeFirst()
        val lastToken = tokens.lastOrNull()

        //Get class name of the last returned object
        database.transactional {
            tokens.dropLast(1).forEach {
                preparedStatement("select return_type from doc where source_id = ? and classname = ? and identifier = ?") {
                    val result = executeQuery(sourceType.id, currentClass, it).readOnce() ?: return emptyList()

                    currentClass = result["return_type"]
                }
            }
        }

        //This happens with "TextChannel#getIterableHistory()#", this gets the docs of the return type of getIterableHistory
        if (lastToken?.isEmpty() == true) {
            return listOf(DocResolveResult(currentClass, currentClass))
        }

        //Do a classic search on the latest return type + optionally last token (might be a method or a field)
        return when {
            lastToken != null -> {
                findSignaturesIn(currentClass, lastToken, DocTypes.IDENTIFIERS)
                    //Current class is added because findSignaturesIn doesn't return "identifier", not "full_signature"
                    .map { "$currentClass#${it.identifierOrFullIdentifier}" }
                    //Not showing the parameter names makes it easier for the user to continue using autocompletion with shift+tab
                    // as parameter names breaks the resolver
                    .map { DocResolveResult(it, it) }
            }

            else -> getClasses(currentClass).map { DocResolveResult(it, it) }
        }
    }

    suspend fun reindex(reindexData: ReindexData): DocIndex {
        mutex.withLock {
            logger.info("Re-indexing docs for {}", sourceType.name)

            if (sourceType != DocSourceType.JAVA) {
                logger.info("Clearing cache for {}", sourceType.name)
                PageCache[sourceType].clearCache()
                logger.info("Cleared cache of {}", sourceType.name)
            }

            val docsSession = DocsSession()

            DocIndexWriter(database, docsSession, sourceType, reindexData).doReindex()

            logger.info("Re-indexed docs for {}", sourceType.name)
        }

        System.gc() //Very effective

        return this
    }

    private suspend fun findDoc(
        docType: DocType,
        className: String,
        identifier: String? = null
    ): DocFindData? {
        database.preparedStatement(
            """
                select id, embed, javadoc_link, source_link
                from doc
                where source_id = ?
                  and type = ?
                  and lower(classname) = lower(?)
                  and quote_nullable(identifier) = quote_nullable(?)
                limit 1
            """.trimIndent()) {
            val result = executeQuery(sourceType.id, docType.id, className, identifier).readOnce() ?: return null
            return DocFindData(
                result["id"],
                DocIndexWriter.GSON.fromJson(result.getString("embed"), MessageEmbed::class.java),
                result["javadoc_link"],
                result["source_link"]
            )
        }
    }

    private suspend fun findSeeAlsoReferences(docId: Int): List<SeeAlsoReference> {
        database.preparedStatement(
            """
                select text, link, target_type, full_signature
                from docseealsoreference
                where doc_id = ?
            """.trimIndent()) {
            return executeQuery(docId).map {
                SeeAlsoReference(
                    it.getString("text"),
                    it.getString("link"),
                    TargetType.fromId(it.getInt("target_type")),
                    it.getString("full_signature")
                )
            }
        }
    }

    companion object {
        private val logger = KotlinLogging.logger { }
    }
}
