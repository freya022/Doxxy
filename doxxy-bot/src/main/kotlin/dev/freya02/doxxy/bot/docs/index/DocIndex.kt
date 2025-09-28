package dev.freya02.doxxy.bot.docs.index

import dev.freya02.doxxy.bot.docs.DocResolveChain
import dev.freya02.doxxy.bot.docs.DocSourceType
import dev.freya02.doxxy.bot.docs.DocWebServer
import dev.freya02.doxxy.bot.docs.cached.CachedClass
import dev.freya02.doxxy.bot.docs.cached.CachedDoc
import dev.freya02.doxxy.bot.docs.cached.CachedField
import dev.freya02.doxxy.bot.docs.cached.CachedMethod
import dev.freya02.doxxy.bot.docs.javadocArchivePath
import dev.freya02.doxxy.bot.docs.metadata.ImplementationIndex
import dev.freya02.doxxy.docs.PageCache
import dev.freya02.doxxy.docs.sections.SeeAlso.SeeAlsoReference
import dev.freya02.doxxy.docs.sections.SeeAlso.TargetType
import io.github.freya022.botcommands.api.core.db.Database
import io.github.freya022.botcommands.api.core.db.preparedStatement
import io.github.freya022.botcommands.api.core.db.transactional
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import net.dv8tion.jda.api.utils.data.DataObject
import org.intellij.lang.annotations.Language

private val logger = KotlinLogging.logger { }
private val memberDelimiters = charArrayOf('#', '.')

//Initial construct just allows database access
// Further updates must be invoked by external methods such as version checkers
class DocIndex(val sourceType: DocSourceType, private val database: Database) : IDocIndex {
    private val mutex = Mutex()

    val implementationIndex = ImplementationIndex(this, database)

    override suspend fun hasClassDoc(className: String): Boolean {
        return database.preparedStatement(
            """
                select *
                from declaration
                where source_id = ?
                  and classname = ?
                limit 1
            """.trimIndent(), readOnly = true
        ) {
            executeQuery(sourceType.id, className).readOrNull() != null
        }
    }

    override suspend fun hasMethodDoc(className: String, signature: String): Boolean {
        return database.preparedStatement(
            """
                select *
                from declaration
                where source_id = ?
                  and classname = ?
                  and identifier = ?
                limit 1
            """.trimIndent(), readOnly = true
        ) {
            executeQuery(sourceType.id, className, signature).readOrNull() != null
        }
    }

    override suspend fun getClassDoc(className: String): CachedClass? {
        val (docId, embed, sourceLink) = findDoc(DocType.CLASS, className) ?: return null
        val seeAlsoReferences: List<SeeAlsoReference> = findSeeAlsoReferences(docId)
        val subclasses = implementationIndex.getSubclasses(className)
        val superclasses = implementationIndex.getSuperclasses(className)

        return CachedClass(this, className, embed, seeAlsoReferences, sourceLink, subclasses, superclasses)
    }

    override suspend fun getMethodDoc(className: String, identifier: String): CachedMethod? {
        val (docId, embed, sourceLink) = findDoc(DocType.METHOD, className, identifier) ?: return null
        val seeAlsoReferences: List<SeeAlsoReference> = findSeeAlsoReferences(docId)
        val implementations = implementationIndex.getImplementations(className, identifier)
        val overriddenMethods = implementationIndex.getOverriddenMethods(className, identifier)

        return CachedMethod(this, className, identifier, embed, seeAlsoReferences, sourceLink, implementations, overriddenMethods)
    }

    override suspend fun getFieldDoc(className: String, fieldName: String): CachedField? {
        val (docId, embed, sourceLink) = findDoc(DocType.FIELD, className, fieldName) ?: return null
        val seeAlsoReferences: List<SeeAlsoReference> = findSeeAlsoReferences(docId)

        return CachedField(this, className, fieldName, embed, seeAlsoReferences, sourceLink)
    }

    override suspend fun findSignaturesIn(className: String, query: String?, docTypes: DocTypes, limit: Int): List<DocSearchResult> {
        @Language("PostgreSQL", prefix = "select * from declaration ")
        val sort = when {
            query.isNullOrEmpty() -> "order by identifier"
            else -> "order by similarity(identifier_no_args, ?) desc"
        }

        val sortArgs = when {
            query.isNullOrEmpty() -> arrayOf()
            else -> arrayOf(query)
        }

        database.preparedStatement(
            """
                select full_identifier, human_identifier, human_class_identifier, return_type
                from fully_qualified_declarations
                where source_id = ?
                  and type = any (?)
                  and classname = ?
                $sort
                limit ?
            """.trimIndent(),
            readOnly = true
        ) {
            return executeQuery(sourceType.id, docTypes.map { it.id }.toTypedArray(), className, *sortArgs, limit)
                .map { DocSearchResult(it) }
        }
    }

    override suspend fun getClasses(query: String?, limit: Int): List<String> {
        @Language("PostgreSQL", prefix = "select * from declaration ")
        val limitingSort = when {
            query.isNullOrEmpty() -> "order by classname limit ?"
            else -> "order by similarity(classname, ?) desc limit ?"
        }

        val sortArgs: Array<*> = when {
            query.isNullOrEmpty() -> arrayOf(limit)
            else -> arrayOf<Any>(query, limit)
        }

        return database.preparedStatement(
            """
            select classname
            from declaration
            where source_id = ?
              and type = ${DocType.CLASS.id}
            $limitingSort
            """.trimIndent()
        ) {
            executeQuery(sourceType.id, *sortArgs).map { it["classname"] }
        }
    }

    override suspend fun resolveDoc(chain: DocResolveChain): CachedDoc? {
        val qualifiedSignature = chain.lastQualifiedSignature
        return when {
            '(' in qualifiedSignature -> getMethodDoc(qualifiedSignature)
            '#' in qualifiedSignature -> getFieldDoc(qualifiedSignature)
            else -> getClassDoc(qualifiedSignature)
        }
    }

    override suspend fun resolveDocAutocomplete(chain: DocResolveChain): List<DocSearchResult> {
        //Find back last search result
        val lastFullIdentifier = chain.secondLastQualifiedSignatureOrNull
            ?: return search(chain.lastQualifiedSignature)

        // Query next methods
        val lastDeclarationParts = lastFullIdentifier.split('#')
        if (lastDeclarationParts.size != 2) return search(chain.lastQualifiedSignature)

        val type: String? = database.preparedStatement(
            """
                select coalesce(return_type, classname) as type
                from fully_qualified_declarations
                where source_id = ?
                  and classname = ?
                  and identifier = ?
                limit 1
            """.trimIndent(), readOnly = true
        ) {
            val (className, memberName) = lastDeclarationParts
            executeQuery(sourceType.id, className, memberName).readOrNull()?.getString("type")
        }

        return if (type != null)
            search("$type#${chain.lastSignature}")
        else
            search(chain.lastQualifiedSignature)
    }

    override suspend fun search(query: String): List<DocSearchResult> {
        if (query.isEmpty()) return emptyList()

        val memberDelimiterIdx = query.indexOfAny(memberDelimiters)
        if (memberDelimiterIdx != -1) {
            // Find method or field in a class

            // If the class name has an exact match
            val className = query.take(memberDelimiterIdx)
            val memberName = query.drop(memberDelimiterIdx + 1)
            val isExactClassName = database.preparedStatement("select id from declaration where source_id = ? and classname = ? limit 1", readOnly = true) {
                executeQuery(sourceType.id, className).any()
            }
            if (isExactClassName) {
                return findSignaturesIn(className, memberName, DocTypes.IDENTIFIERS, limit = 25)
            }

            return database.transactional(readOnly = true) {
                preparedStatement("set pg_trgm.similarity_threshold = 0.2;") { executeUpdate() }

                preparedStatement("""
                    SELECT
                        * 
                    FROM
                        search_members(?, ?, ?)
                    ORDER BY
                        similarity DESC,
                        full_identifier
                    LIMIT ${OptionData.MAX_CHOICES};
                """.trimIndent()) {
                    executeQuery(className, memberName, sourceType.id).map { DocSearchResult(it) }
                }
            }
        } else {
            // Find anything

            return database.transactional(readOnly = true) {
                preparedStatement("set pg_trgm.similarity_threshold = 0.1;") { executeUpdate() }

                val inferredTypes = when {
                    query.all { it.isUpperCase() } -> DocTypes.FIELD
                    else -> DocTypes(DocType.CLASS, DocType.METHOD)
                }

                val searchQuery = inferredTypes.joinToString(separator = "\nUNION ALL\n") { type ->
                    """
                        SELECT
                            *
                        FROM
                            search_declarations(?, _source_id := ?, _type := ${type.id})
                    """.trimIndent()
                }

                preparedStatement("""
                    $searchQuery
                    ORDER BY
                        similarity DESC,
                        full_identifier
                    LIMIT ${OptionData.MAX_CHOICES};
                """.trimIndent()) {
                    repeat(inferredTypes.size) {
                        setString((it * 2) + 1, query)
                        setInt((it * 2) + 2, sourceType.id)
                    }

                    executeQuery().map { DocSearchResult(it) }
                }
            }
        }
    }

    suspend fun reindex(reindexData: ReindexData): DocIndex {
        mutex.withLock {
            logger.info { "Re-indexing docs for ${sourceType.name}" }

            if (sourceType != DocSourceType.JAVA) {
                logger.info { "Clearing cache for ${sourceType.name}" }
                PageCache[sourceType.name].clearCache()
                logger.info { "Cleared cache of ${sourceType.name}" }
            }

            if (sourceType == DocSourceType.JDA) {
                DocWebServer.withLocalJavadocsWebServer("/JDA", sourceType.javadocArchivePath) {
                    DocIndexWriter(database, sourceType, reindexData).doReindex()
                }
            } else {
                DocIndexWriter(database, sourceType, reindexData).doReindex()
            }

            logger.info { "Re-indexed docs for ${sourceType.name}" }
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
                select id, embed, source_link
                from declaration
                join javadoc using (javadoc_id)
                where source_id = ?
                  and type = ?
                  and lower(classname) = lower(?)
                  and quote_nullable(identifier) = quote_nullable(?)
                limit 1
            """.trimIndent()) {
            val result = executeQuery(sourceType.id, docType.id, className, identifier).readOrNull() ?: return null
            return DocFindData(
                result["id"],
                result.getString("embed").let(DataObject::fromJson).let(EmbedBuilder::fromData).build(),
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
}
