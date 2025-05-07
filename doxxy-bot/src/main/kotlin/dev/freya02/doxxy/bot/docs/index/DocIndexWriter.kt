package dev.freya02.doxxy.bot.docs.index

import com.github.javaparser.symbolsolver.javaparsermodel.JavaParserFacade
import dev.freya02.doxxy.bot.docs.DocEmbeds.toEmbed
import dev.freya02.doxxy.bot.docs.metadata.parser.ImplementationMetadataWriter
import dev.freya02.doxxy.bot.docs.metadata.parser.SourceRootMetadata
import dev.freya02.doxxy.docs.ClassDocs
import dev.freya02.doxxy.docs.DocSourceType
import dev.freya02.doxxy.docs.DocUtils.returnTypeNoAnnotations
import dev.freya02.doxxy.docs.DocsSession
import dev.freya02.doxxy.docs.data.BaseDoc
import dev.freya02.doxxy.docs.data.ClassDetailType
import dev.freya02.doxxy.docs.data.ClassDoc
import dev.freya02.doxxy.docs.sourceDirectory
import io.github.freya022.botcommands.api.core.db.Database
import io.github.freya022.botcommands.api.core.db.Transaction
import io.github.freya022.botcommands.api.core.db.preparedStatement
import io.github.freya022.botcommands.api.core.db.transactional
import io.github.oshai.kotlinlogging.KotlinLogging
import net.dv8tion.jda.api.utils.data.DataObject

private val logger = KotlinLogging.logger { }

internal class DocIndexWriter(
    private val database: Database,
    private val docsSession: DocsSession,
    private val sourceType: DocSourceType,
    private val reindexData: ReindexData
) {
    private val annotationRegex: Regex = "@\\w+ ".toRegex()
    private val sourceRootMetadata: SourceRootMetadata? = sourceType.sourceDirectory?.let { SourceRootMetadata(it) }

    suspend fun doReindex() {
        val updatedSource = ClassDocs.getUpdatedSource(sourceType)

        database.transactional {
            sourceRootMetadata?.let { sourceRootMetadata ->
                ImplementationMetadataWriter.reindex(sourceType, reindexData, sourceRootMetadata)
            }

            //This would clean type solvers stored in a static WeakHashMap
            // But since it's a WeakHashMap, the GC should reclaim space if it becomes insufficient
            JavaParserFacade.clearInstances()

            System.gc() //600 MB -> 30 MB

            preparedStatement("delete from doc where source_id = ?") {
                executeUpdate(sourceType.id)
            }

            for ((className, classUrl) in updatedSource.simpleNameToUrlMap) {
                try {
                    val classDoc = docsSession.retrieveDoc(classUrl)

                    if (classDoc == null) {
                        logger.warn { "Unable to get docs of '${className}' at '${classUrl}', javadoc version or source type may be incorrect" }
                        continue
                    }

                    val classEmbed = toEmbed(classDoc).build()
                    val classEmbedJson = classEmbed.toData()
                    val sourceLink = reindexData.getClassSourceUrlOrNull(classDoc)

                    val classDocId = insertDoc(DocType.CLASS, classDoc.className, classDoc, classEmbedJson, sourceLink)
                    insertSeeAlso(classDoc, classDocId)

                    insertMethodDocs(classDoc, sourceLink)
                    insertFieldDocs(classDoc, sourceLink)
                } catch (e: Exception) {
                    throw RuntimeException("An exception occurred while reading the docs of '$className' at '$classUrl'", e)
                }
            }

            preparedStatement("refresh materialized view doc_view") {
                executeUpdate()
            }
        }.also {
            database.preparedStatement("vacuum analyse") {
                executeUpdate()
            }
        }
    }

    context(_: Transaction)
    private suspend fun insertMethodDocs(classDoc: ClassDoc, sourceLink: String?) {
        for (methodDoc in classDoc.getMethodDocs().values) {
            try {
                val methodEmbed = toEmbed(classDoc, methodDoc).build()
                val methodEmbedJson = methodEmbed.toData()

                val methodRange: IntRange? = when (sourceLink) {
                    null -> null
                    else -> sourceRootMetadata?.let { sourceRootMetadata ->
                        val docsParametersString = methodDoc.methodParameters
                            ?.asString
                            ?.drop(1)
                            ?.dropLast(1)
                            ?.replace(annotationRegex, "")
                            ?: ""

                        if (docsParametersString.isEmpty() && methodDoc.classDetailType == ClassDetailType.CONSTRUCTOR) {
                            return@let null
                        } else if (docsParametersString.isEmpty() && methodDoc.classDetailType == ClassDetailType.ANNOTATION_ELEMENT) {
                            return@let null
                        } else if (docsParametersString.contains("net.dv8tion.jda.internal")) {
                            return@let null
                        } else if (docsParametersString.contains("okhttp3")) {
                            return@let null
                        } else if (docsParametersString.contains("gnu.")) {
                            return@let null
                        } else {
                            if (docsParametersString.isEmpty() && methodDoc.methodName == "values"
                                && methodDoc.classDocs.enumConstants.isNotEmpty()
                            ) {
                                return@let null
                            } else if (docsParametersString == "String name" && methodDoc.methodName == "valueOf"
                                && methodDoc.classDocs.enumConstants.isNotEmpty()
                            ) {
                                return@let null
                            }
                        }

                        val range: IntRange? = sourceRootMetadata
                            .getMethodsParameters(methodDoc.classDocs.classNameFqcn, methodDoc.methodName)
                            .find { it.parametersString == docsParametersString }
                            ?.methodRange

                        if (range != null) return@let range

                        logger.warn { "Method not found: ${methodDoc.methodSignature}" }

                        null
                    }
                }

                val methodClassSourceLink = reindexData.getClassSourceUrlOrNull(methodDoc.classDocs)
                val methodLink = when (methodRange) {
                    null -> null
                    else -> "$methodClassSourceLink#L${methodRange.first}-L${methodRange.last}"
                }

                val methodDocId = insertDoc(DocType.METHOD, classDoc.className, methodDoc, methodEmbedJson, methodLink)
                insertSeeAlso(methodDoc, methodDocId)
            } catch (e: Exception) {
                throw RuntimeException(
                    "An exception occurred while reading the docs of " + classDoc.className + "#" + methodDoc.simpleSignature,
                    e
                )
            }
        }
    }

    context(_: Transaction)
    private suspend fun insertFieldDocs(classDoc: ClassDoc, sourceLink: String?) {
        for (fieldDoc in classDoc.getFieldDocs().values) {
            try {
                val fieldEmbed = toEmbed(classDoc, fieldDoc).build()
                val fieldEmbedJson = fieldEmbed.toData()

                val fieldRange: IntRange? = when (sourceLink) {
                    null -> null
                    else -> sourceRootMetadata?.let { sourceRootMetadata ->
                        val range: IntRange? = sourceRootMetadata
                            .getFieldMetadata(fieldDoc.classDocs.classNameFqcn, fieldDoc.fieldName)
                            ?.fieldRange

                        if (range != null) return@let range

                        logger.warn { "Field not found: ${fieldDoc.classDocs.className}#${fieldDoc.simpleSignature}" }

                        null
                    }
                }

                val fieldClassSourceLink = reindexData.getClassSourceUrlOrNull(fieldDoc.classDocs)
                val fieldLink = when (fieldRange) {
                    null -> null
                    else -> "$fieldClassSourceLink#L${fieldRange.first}-L${fieldRange.last}"
                }

                val fieldDocId = insertDoc(DocType.FIELD, classDoc.className, fieldDoc, fieldEmbedJson, fieldLink)
                insertSeeAlso(fieldDoc, fieldDocId)
            } catch (e: Exception) {
                throw RuntimeException(
                    "An exception occurred while reading the docs of " + classDoc.className + "#" + fieldDoc.simpleSignature,
                    e
                )
            }
        }
    }

    context(transaction: Transaction)
    private suspend fun insertDoc(
        docType: DocType,
        className: String,
        baseDoc: BaseDoc,
        embedJson: DataObject,
        sourceLink: String?
    ): Int {
        return transaction.preparedStatement(
            """
            insert into doc (source_id, type, classname, identifier, identifier_no_args, human_identifier, human_class_identifier,
                             return_type, embed, javadoc_link, source_link)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            returning id""".trimIndent()
        ) {
            executeQuery(
                sourceType.id,
                docType.id,
                className,
                baseDoc.identifier,
                baseDoc.identifierNoArgs,
                baseDoc.humanIdentifier,
                baseDoc.toHumanClassIdentifier(className),
                baseDoc.returnTypeNoAnnotations,
                embedJson.toString(),
                baseDoc.onlineURL,
                sourceLink
            ).read()["id"]
        }
    }

    context(transaction: Transaction)
    private suspend fun insertSeeAlso(baseDoc: BaseDoc, docId: Int) {
        baseDoc.seeAlso?.getReferences()?.forEach { seeAlsoReference ->
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
}
