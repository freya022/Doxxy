package dev.freya02.doxxy.bot.docs.index

import com.github.javaparser.symbolsolver.javaparsermodel.JavaParserFacade
import dev.freya02.doxxy.bot.docs.DocEmbeds.toEmbed
import dev.freya02.doxxy.bot.docs.DocSourceType
import dev.freya02.doxxy.bot.docs.metadata.parser.ImplementationMetadataWriter
import dev.freya02.doxxy.bot.docs.metadata.parser.SourceMetadata
import dev.freya02.doxxy.bot.docs.sourceDirectoryPath
import dev.freya02.doxxy.docs.GlobalJavadocSession
import dev.freya02.doxxy.docs.JavadocSource
import dev.freya02.doxxy.docs.JavadocSources
import dev.freya02.doxxy.docs.declarations.*
import dev.freya02.doxxy.docs.sections.ClassDetailType
import io.github.freya022.botcommands.api.core.db.Database
import io.github.freya022.botcommands.api.core.db.Transaction
import io.github.freya022.botcommands.api.core.db.preparedStatement
import io.github.freya022.botcommands.api.core.db.transactional
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.dv8tion.jda.api.utils.data.DataObject

private val logger = KotlinLogging.logger { }

internal class DocIndexWriter(
    private val database: Database,
    private val sourceType: DocSourceType,
    private val reindexData: ReindexData
) {
    private val sourceMetadata: SourceMetadata? = sourceType.sourceDirectoryPath?.let { SourceMetadata(it) }

    private val memberJavadocs = MemberJavadocs()

    suspend fun doReindex() {
        val globalJavadocSession = GlobalJavadocSession(JavadocSources(DocSourceType.entries.map { type ->
            JavadocSource(type.name, type.sourceUrl, type.onlineURL, type.packageMatchers.toList())
        }))
        val javadocModuleSession = globalJavadocSession.retrieveSession(globalJavadocSession.sources.getByName(sourceType.name)!!)

        database.transactional {
            sourceMetadata?.let { sourceRootMetadata ->
                ImplementationMetadataWriter.reindex(sourceType, reindexData, sourceRootMetadata)
            }

            //This would clean type solvers stored in a static WeakHashMap
            // But since it's a WeakHashMap, the GC should reclaim space if it becomes insufficient
            JavaParserFacade.clearInstances()

            System.gc() //600 MB -> 30 MB

            val javadocIds = preparedStatement("delete from declaration where source_id = ? returning javadoc_id") {
                executeQuery(sourceType.id).mapTo(hashSetOf()) { it.getInt(1) }.toIntArray()
            }

            preparedStatement("DELETE FROM javadoc WHERE javadoc_id = any(?)") {
                executeUpdate(javadocIds)
            }

            javadocModuleSession
                .classesAsFlow()
                .flowOn(Dispatchers.IO.limitedParallelism(parallelism = 8, name = "Document fetch"))
                .buffer()
                .collect { javadocClass ->
                    try {
                        val classEmbed = toEmbed(javadocClass)
                        val classEmbedJson = classEmbed.toData()
                        val baseLink = reindexData.getClassSourceUrlOrNull(javadocClass)
                        val sourceLink = baseLink?.let { javadocClass.getRangedLink(it) }

                        val javadocId = insertJavadoc(classEmbedJson)
                        val classDocId = insertDeclaration(DocType.CLASS, javadocClass.className, javadocClass, sourceLink, javadocId)
                        insertSeeAlso(javadocClass, classDocId)

                        insertMethodDocs(javadocClass, baseLink)
                        insertFieldDocs(javadocClass, baseLink)
                    } catch (e: Exception) {
                        throw RuntimeException("An exception occurred while reading the docs of '${javadocClass.sourceURL}'", e)
                    }
                }
        }.also {
            database.preparedStatement("vacuum analyse") {
                executeUpdate()
            }
        }
    }

    private fun JavadocClass.getRangedLink(baseLink: String): String {
        if (sourceMetadata == null) return baseLink
        val metadata = sourceMetadata.getClassMetadata(classNameFqcn)
        if (metadata == null) {
            logger.warn { "Class metadata not found: $classNameFqcn" }
            return baseLink
        }

        // Don't put range on top-level classes as it would select most of the file
        if (metadata.enclosedBy == null) return baseLink

        return "$baseLink#L${metadata.range.first}-L${metadata.range.last}"
    }

    context(_: Transaction)
    private suspend fun insertMethodDocs(clazz: JavadocClass, sourceLink: String?) {
        for (method in clazz.methods.values) {
            try {
                val methodLink: String? = method.getLinkOrNull(sourceLink)

                val javadocId = memberJavadocs.getOrInsert(method) { insertJavadoc(toEmbed(method).toData()) }
                val methodId = insertDeclaration(DocType.METHOD, clazz.className, method, methodLink, javadocId)
                insertSeeAlso(method, methodId)
            } catch (e: Exception) {
                throw RuntimeException(
                    "An exception occurred while reading the docs of " + clazz.className + "#" + method.simpleSignature,
                    e
                )
            }
        }
    }

    private fun JavadocMethod.getLinkOrNull(sourceLink: String?): String? {
        if (sourceLink == null || sourceMetadata == null) return null
        if (isBuiltInEnumMethod()) return null

        val methodRange: IntRange? = sourceMetadata.getMethodRange(this)
        if (methodRange == null) {
            if (classDetailType == ClassDetailType.CONSTRUCTOR && parameters.isEmpty())
                return null
            logger.warn { "Method not found: $methodSignature" }
            return null
        }

        // This is different from `sourceLink` as it could come from a superclass
        val methodClassSourceLink = reindexData.getClassSourceUrlOrNull(declaringClass)
        return "$methodClassSourceLink#L${methodRange.first}-L${methodRange.last}"
    }

    context(_: Transaction)
    private suspend fun insertFieldDocs(clazz: JavadocClass, sourceLink: String?) {
        for (field in clazz.fields.values) {
            try {
                val fieldLink: String? = field.getLinkOrNull(sourceLink)

                val javadocId = memberJavadocs.getOrInsert(field) { insertJavadoc(toEmbed(field).toData()) }
                val fieldId = insertDeclaration(DocType.FIELD, clazz.className, field, fieldLink, javadocId)
                insertSeeAlso(field, fieldId)
            } catch (e: Exception) {
                throw RuntimeException(
                    "An exception occurred while reading the docs of " + clazz.className + "#" + field.simpleSignature,
                    e
                )
            }
        }
    }

    private fun JavadocField.getLinkOrNull(sourceLink: String?): String? {
        if (sourceLink == null) return null
        if (sourceMetadata == null) return null

        val fieldRange: IntRange? = sourceMetadata.getFieldRange(this)
        if (fieldRange == null) {
            logger.warn { "Field not found: ${declaringClass.className}#$fieldName" }
            return null
        }

        // This is different from `sourceLink` as it could come from a superclass
        val fieldClassSourceLink = reindexData.getClassSourceUrlOrNull(declaringClass)
        return "$fieldClassSourceLink#L${fieldRange.first}-L${fieldRange.last}"
    }

    context(transaction: Transaction)
    private suspend fun insertJavadoc(embedObj: DataObject): Int {
        return transaction.preparedStatement("insert into javadoc (embed) values (?) returning javadoc_id") {
            executeQuery(embedObj.toString()).read().getInt("javadoc_id")
        }
    }

    context(transaction: Transaction)
    private suspend fun insertDeclaration(
        docType: DocType,
        className: String,
        javadoc: AbstractJavadoc,
        sourceLink: String?,
        javadocId: Int,
    ): Int {
        return transaction.preparedStatement(
            """
            insert into declaration (source_id, type, classname, identifier, identifier_no_args, human_identifier, human_class_identifier,
                                    return_type, source_link, javadoc_id)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            returning id""".trimIndent()
        ) {
            executeQuery(
                sourceType.id,
                docType.id,
                className,
                javadoc.identifier,
                javadoc.identifierNoArgs,
                javadoc.humanIdentifier,
                javadoc.toHumanClassIdentifier(className),
                javadoc.returnTypeNoAnnotations,
                sourceLink,
                javadocId,
            ).read().getInt("id")
        }
    }

    context(transaction: Transaction)
    private suspend fun insertSeeAlso(javadoc: AbstractJavadoc, docId: Int) {
        javadoc.seeAlso?.references?.forEach { seeAlsoReference ->
            transaction.preparedStatement("insert into docseealsoreference (doc_id, text, link, target_type, full_signature) VALUES (?, ?, ?, ?, ?)") {
                executeUpdate(
                    docId,
                    seeAlsoReference.text,
                    seeAlsoReference.link,
                    seeAlsoReference.targetType.id,
                    seeAlsoReference.fullSignature
                )
            }
        }
    }

    private class MemberJavadocs {

        private val javadocIds: MutableMap<String, Int> = hashMapOf()
        private val locks: MutableMap<String, Mutex> = hashMapOf()

        suspend fun getOrInsert(javadoc: AbstractJavadoc, onInsert: suspend () -> Int): Int {
            // Important that we use the class name of the member,
            // as we want to retrieve the javadoc ID of the class that declared the member's docs,
            // to reuse it.
            val qualifiedMember = "${javadoc.className}#${javadoc.identifier}"
            val mutex = synchronized(locks) { locks.computeIfAbsent(qualifiedMember) { Mutex() } }
            return mutex.withLock {
                javadocIds.getOrPut(qualifiedMember) { onInsert() }
            }
        }
    }
}

private fun JavadocMethod.isBuiltInEnumMethod(): Boolean {
    // Technically this isn't entirely accurate, but who leaves empty enums?
    if (declaringClass.enumConstants.isEmpty()) return false

    // values()
    if (methodName == "values" && parameters.isEmpty()) return true

    // valueOf(java.lang.String)
    if (methodName == "valueOf" &&
        parameters.size == 1 &&
        parameters[0].type == "java.lang.String"
    ) return true

    return false
}

private fun SourceMetadata.getMethodRange(method: JavadocMethod): IntRange? {
    return getMethodsParameters(method.declaringClass.classNameFqcn, method.methodName)
        .find { it.types == method.parameters.map(MethodDocParameter::type) }
        ?.methodRange
}

private fun SourceMetadata.getFieldRange(field: JavadocField): IntRange? {
    return getFieldMetadata(field.declaringClass.classNameFqcn, field.fieldName)?.fieldRange
}
