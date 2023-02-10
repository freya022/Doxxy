package com.freya02.bot.docs.index

import com.freya02.bot.docs.cached.CachedClass
import com.freya02.bot.docs.cached.CachedDoc
import com.freya02.bot.docs.cached.CachedField
import com.freya02.bot.docs.cached.CachedMethod
import com.freya02.botcommands.api.core.db.Database
import com.freya02.botcommands.api.core.db.KConnection
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
//TODO Improve indexing speed by disabling indexes, see https://www.postgresql.org/docs/current/populate.html
//TODO Improve search speeds by using indexes on classname, identifier_no_args, full_identifier, see https://www.postgresql.org/docs/current/pgtrgm.html#id-1.11.7.44.8
// Example on L352 in console
/*
create index doc_view_identifier_no_args_gist on doc_view using gist(full_identifier gist_trgm_ops(siglen=256));

set pg_trgm.similarity_threshold = 0.1;

-- Performance optimized
-- The trick may be to set a lower similarity threshold as to get more, but similar enough results
--   And then filter with the accurate similarity on the remaining rows
EXPLAIN ANALYSE
SELECT *
FROM (SELECT coalesce(full_identifier, classname)                                    AS full_identifier,
             coalesce(human_identifier, classname)                                   AS human_identifier,
             coalesce(human_class_identifier, classname)                             AS human_class_identifier,
             similarity('Guild', classname) * similarity('upda', identifier_no_args) AS overall_similarity,
             type
      FROM doc_view
               NATURAL JOIN doc --The join order is important
      WHERE source_id = 1
        AND full_identifier % 'Guil#upda' -- Uses a fake threshold of 0.1, set above
     ) as search
WHERE overall_similarity > 0.22 --Real threshold
ORDER BY CASE WHEN NOT 'Guil#upda' LIKE '%#%' THEN type END, --Don't order by type if the query asks for identifiers of a class
         overall_similarity DESC NULLS LAST,
         full_identifier                                     --Class > Method > Field, then similarity
LIMIT 25;

-- Result optimized
explain analyse
select *
from (select coalesce(full_identifier, classname)                                    as full_identifier,
             coalesce(human_identifier, classname)                                   as human_identifier,
             coalesce(human_class_identifier, classname)                             as human_class_identifier,
             similarity('Guild', classname) * similarity('upda', identifier_no_args) as overall_similarity,
             type
      from doc
               natural left join doc_view) as d
where overall_similarity > 0.22
  and not full_identifier = any (ARRAY []::text[]) --Remove previous results
order by case when not 'Guil#upda' like '%#%' then type end, --Don't order by type if the query asks for identifiers of a class
         overall_similarity desc nulls last,
         full_identifier                                     --Class > Method > Field, then similarity
limit 25;
 */
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

    override suspend fun findAnySignatures(query: String, limit: Int, docTypes: DocTypes) =
        database.withConnection(readOnly = true) {
            preparedStatement("set pg_trgm.similarity_threshold = 0.1;") { executeUpdate(*emptyArray()) }
            findAnySignatures0(query, limit, docTypes)
        }

    override suspend fun findSignaturesIn(className: String, query: String?, docTypes: DocTypes, limit: Int): List<DocSearchResult> {
        @Language("PostgreSQL", prefix = "select * from doc ")
        val sort = when {
            query.isNullOrEmpty() -> "order by identifier"
            else -> "order by similarity(identifier_no_args, ?) desc"
        }

        val sortArgs = when {
            query.isNullOrEmpty() -> arrayOf()
            else -> arrayOf(query.lowercase())
        }

        database.preparedStatement("""
                select full_identifier, human_identifier, human_class_identifier
                from doc natural join doc_view
                where source_id = ?
                  and type = any (?)
                  and classname = ?
                $sort
                limit ?
                """.trimIndent()
        ) {
            return executeQuery(sourceType.id, docTypes.map { it.id }.toTypedArray(), className, *sortArgs, limit)
                .map { DocSearchResult(it) }
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
            executeQuery(sourceType.id, DocType.CLASS.id, *sortArgs).map { it["classname"] }
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
                    .map { it.fullIdentifier }
                    //Not showing the parameter names makes it easier for the user to continue using autocompletion with shift+tab
                    // as parameter names breaks the resolver
                    .map { DocResolveResult(it, it) }
            }

            else -> getClasses(currentClass).map { DocResolveResult(it, it) }
        }
    }

    //Performance optimized
    //  The trick may be to set a lower similarity threshold as to get more, but similar enough results
    //  And then filter with the accurate similarity on the remaining rows
    override suspend fun experimentalSearch(query: String): List<DocSearchResult> = database.withConnection(readOnly = true) {
        preparedStatement("set pg_trgm.similarity_threshold = 0.1;") { executeUpdate(*emptyArray()) }

        val results = findAnySignatures0(query, limit = 5, DocTypes.ANY)

        return withSimilarityScoreMethod(query) { similarityScoreQuery, similarityScoreQueryParams ->
            results + preparedStatement("""
                select *
                from (select coalesce(full_identifier, classname)        as full_identifier,
                             coalesce(human_identifier, classname)       as human_identifier,
                             coalesce(human_class_identifier, classname) as human_class_identifier,
                             $similarityScoreQuery                       as overall_similarity,
                             type
                      from doc_view
                               natural left join doc
                      where source_id = ?
                        and full_identifier % ? -- Uses a fake threshold of 0.1, set above
                     ) as low_accuracy_search
                where overall_similarity > 0.22     --Real threshold
                  and not full_identifier = any (?) --Remove previous results
                order by case when not ? like '%#%' then type end, --Don't order by type if the query asks for identifiers of a class 
                         overall_similarity desc nulls last,
                         full_identifier                           --Class > Method > Field, then similarity
                limit ?;
            """.trimIndent()) {
                executeQuery(*similarityScoreQueryParams, sourceType.id, query, results.map { it.fullIdentifier }.toTypedArray(), query, 25 - results.size)
                    .map { DocSearchResult(it) }
            }
        } ?: emptyList()
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

    //TODO test
    /** **Requires `pg_trgm.similarity_threshold` to be set**  */
    context(KConnection)
    private suspend fun findAnySignatures0(query: String, limit: Int, docTypes: DocTypes): List<DocSearchResult> {
        if (docTypes.isEmpty()) throw IllegalArgumentException("Must have at least one doc type")

        return withSimilarityScoreMethod(query) { similarityScoreQuery, similarityScoreQueryParams ->
            preparedStatement("""
                select coalesce(full_identifier, classname)        as full_identifier,
                       coalesce(human_identifier, classname)       as human_identifier,
                       coalesce(human_class_identifier, classname) as human_class_identifier,
                       $similarityScoreQuery                       as overall_similarity
                from doc_view
                         natural left join doc
                where source_id = ?
                -- Uses a fake threshold set by the caller, 
                --  it should be low enough as that the X best values are always displayed, 
                --  but ordered by the accurate score
                  and ? % full_identifier
                order by overall_similarity desc nulls last, full_identifier
                limit ?;
            """.trimIndent()) {
                executeQuery(*similarityScoreQueryParams, sourceType.id, query, limit).map { DocSearchResult(it) }
            }
        } ?: return emptyList()
    }

    private suspend fun withSimilarityScoreMethod(
        query: String,
        block: suspend (String, Array<out Any>) -> List<DocSearchResult>
    ): List<DocSearchResult>? {
        val matchResult = queryRegex.matchEntire(query) ?: return null
        val (entireMatch, classname, identifier) = matchResult.groupValues

        @Language("PostgreSQL", prefix = "select ", suffix = " from doc")
        val similarityScoreQuery = when {
            //Guild#updateCommands, find with both columns
            classname.isNotBlank() && identifier.isNotBlank() -> "similarity(?, classname) * similarity(?, identifier_no_args)"
            //Guild#, find all methods of that class
            classname.isNotBlank() && '#' in entireMatch -> "similarity(?, classname)"
            //updateCommands or Guild, get the best similarity between class and identifier
            classname.isNotBlank() -> "greatest(similarity(?, classname), similarity(?, identifier_no_args))"
            //#updateCommands, get the best similarity in identifiers
            identifier.isNotBlank() -> "similarity(?, identifier_no_args)"
            else -> "0" //No input
        }
        val similarityScoreQueryParams = when {
            classname.isNotBlank() && identifier.isNotBlank() -> arrayOf(classname, identifier)
            classname.isNotBlank() && '#' in entireMatch -> arrayOf(classname)
            classname.isNotBlank() -> arrayOf(classname, classname)
            identifier.isNotBlank() -> arrayOf(identifier)
            else -> arrayOf()
        }

        return block(similarityScoreQuery, similarityScoreQueryParams)
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

        private val queryRegex = Regex("""^(\w*)#?(\w*)""")
    }
}
