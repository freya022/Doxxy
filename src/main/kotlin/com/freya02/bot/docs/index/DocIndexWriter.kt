package com.freya02.bot.docs.index

import com.freya02.bot.Data
import com.freya02.bot.docs.DocEmbeds.toEmbed
import com.freya02.bot.docs.metadata.ImplementationMetadataWriter
import com.freya02.bot.docs.metadata.SourceRootMetadata
import com.freya02.botcommands.api.core.db.Database
import com.freya02.botcommands.api.core.db.Transaction
import com.freya02.docs.ClassDocs
import com.freya02.docs.DocSourceType
import com.freya02.docs.DocUtils.returnTypeNoAnnotations
import com.freya02.docs.DocsSession
import com.freya02.docs.data.BaseDoc
import com.freya02.docs.data.ClassDetailType
import com.freya02.docs.data.ClassDoc
import com.google.gson.GsonBuilder
import mu.KotlinLogging
import net.dv8tion.jda.api.entities.MessageEmbed

internal class DocIndexWriter(
    private val database: Database,
    private val docsSession: DocsSession,
    private val sourceType: DocSourceType,
    private val reindexData: ReindexData
) {
    private val annotationRegex: Regex = "@\\w+ ".toRegex()
    private val sourceRootMetadata: SourceRootMetadata? = sourceType.sourceFolderName?.let { docsFolderName ->
        SourceRootMetadata(Data.javadocsPath.resolve(docsFolderName))
    }

    suspend fun doReindex() = database.transactional {
        sourceRootMetadata?.let { sourceRootMetadata ->
            ImplementationMetadataWriter.reindex(sourceType, reindexData, sourceRootMetadata)
        }

        return@transactional

        val updatedSource = ClassDocs.getUpdatedSource(sourceType)

        preparedStatement("delete from doc where source_id = ?") {
            executeUpdate(sourceType.id)
        }

        for ((className, classUrl) in updatedSource.getSimpleNameToUrlMap()) {
            try {
                val classDoc = docsSession.retrieveDoc(classUrl)

                if (classDoc == null) {
                    logger.warn("Unable to get docs of '${className}' at '${classUrl}', javadoc version or source type may be incorrect")
                    continue
                }

                val classEmbed = toEmbed(classDoc).build()
                val classEmbedJson = GSON.toJson(classEmbed)
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
            executeUpdate(*emptyArray())
        }
    }.also {
        database.preparedStatement("vacuum analyse") {
            executeUpdate(*emptyArray())
        }
    }

    context(Transaction)
    private suspend fun insertMethodDocs(classDoc: ClassDoc, sourceLink: String?) {
        for (methodDoc in classDoc.getMethodDocs().values) {
            try {
                val methodEmbed = toEmbed(classDoc, methodDoc).build()
                val methodEmbedJson = GSON.toJson(methodEmbed)

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

                        logger.warn("Method not found: ${methodDoc.methodSignature}")

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

    context(Transaction)
    private suspend fun insertFieldDocs(classDoc: ClassDoc, sourceLink: String?) {
        for (fieldDoc in classDoc.getFieldDocs().values) {
            try {
                val fieldEmbed = toEmbed(classDoc, fieldDoc).build()
                val fieldEmbedJson = GSON.toJson(fieldEmbed)

                val fieldRange: IntRange? = when (sourceLink) {
                    null -> null
                    else -> sourceRootMetadata?.let { sourceRootMetadata ->
                        val range: IntRange? = sourceRootMetadata
                            .getFieldMetadata(fieldDoc.classDocs.classNameFqcn, fieldDoc.fieldName)
                            ?.fieldRange

                        if (range != null) return@let range

                        logger.warn("Field not found: ${fieldDoc.classDocs.className}#${fieldDoc.simpleSignature}")

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

    context(Transaction)
    private suspend fun insertDoc(
        docType: DocType,
        className: String,
        baseDoc: BaseDoc,
        embedJson: String,
        sourceLink: String?
    ): Int {
        return preparedStatement(
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
                embedJson,
                baseDoc.onlineURL,
                sourceLink
            ).readOnce()!!["id"]
        }
    }

    context(Transaction)
    private suspend fun insertSeeAlso(baseDoc: BaseDoc, docId: Int) {
        baseDoc.seeAlso?.getReferences()?.forEach { seeAlsoReference ->
            preparedStatement("insert into docseealsoreference (doc_id, text, link, target_type, full_signature) VALUES (?, ?, ?, ?, ?)") {
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

    companion object {
        private val logger = KotlinLogging.logger { }

        internal val GSON = GsonBuilder()
            .registerTypeAdapter(MessageEmbed::class.java, MessageEmbedAdapter)
            .create()
    }
}
