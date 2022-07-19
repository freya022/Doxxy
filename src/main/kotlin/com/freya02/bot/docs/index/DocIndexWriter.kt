package com.freya02.bot.docs.index

import com.freya02.bot.Main
import com.freya02.bot.db.Database
import com.freya02.bot.db.Transaction
import com.freya02.bot.docs.DocEmbeds.toEmbed
import com.freya02.bot.docs.metadata.SourceRootMetadata
import com.freya02.botcommands.api.Logging
import com.freya02.docs.ClassDocs
import com.freya02.docs.DocSourceType
import com.freya02.docs.DocsSession
import com.freya02.docs.data.BaseDoc
import com.freya02.docs.data.ClassDetailType
import com.freya02.docs.data.ClassDoc
import com.google.gson.GsonBuilder
import net.dv8tion.jda.api.entities.MessageEmbed

private val LOGGER = Logging.getLogger()

internal class DocIndexWriter(private val database_: Database, private val docsSession: DocsSession, private val sourceType: DocSourceType) {
    private val sourceRootMetadata: SourceRootMetadata?
    private val annotationRegex: Regex = "@\\w+ ".toRegex()

    init {
        val docsFolderName = when (sourceType) {
            DocSourceType.JDA -> "JDA"
            DocSourceType.BOT_COMMANDS -> "BotCommands"
            else -> null
        }

        sourceRootMetadata = when {
            docsFolderName != null -> SourceRootMetadata(Main.JAVADOCS_PATH.resolve(docsFolderName))
            else -> null
        }
    }

    suspend fun doReindex() = database_.transactional {
        val updatedSource = ClassDocs.getUpdatedSource(sourceType)

        preparedStatement("delete from doc where source_id = ?") {
            executeUpdate(sourceType.id)
        }

        for ((className, classUrl) in updatedSource.getSimpleNameToUrlMap()) {
            try {
                val classDoc = docsSession.retrieveDoc(classUrl)

                if (classDoc == null) {
                    LOGGER.warn("Unable to get docs of '${className}' at '${classUrl}', javadoc version or source type may be incorrect")
                    continue
                }

                val classEmbed = toEmbed(classDoc).build()
                val classEmbedJson = GSON.toJson(classEmbed)
                val sourceLink = classDoc.asGithubLink()

                val classDocId = insertDoc(DocType.CLASS, classDoc.className, classDoc, classEmbedJson, sourceLink)
                insertSeeAlso(classDoc, classDocId)

                insertMethodDocs(classDoc, sourceLink)
                insertFieldDocs(classDoc, sourceLink)
            } catch (e: Exception) {
                throw RuntimeException("An exception occurred while reading the docs of '$className' at '$classUrl'", e)
            }
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
                        }  else {
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

                        LOGGER.warn("Method not found: ${methodDoc.methodSignature}")

                        null
                    }
                }

                val methodClassSourceLink = methodDoc.classDocs.asGithubLink()

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

                val fieldDocId = insertDoc(DocType.FIELD, classDoc.className, fieldDoc, fieldEmbedJson, null)
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
        return preparedStatement("insert into doc (source_id, type, classname, identifier, embed, source_link) VALUES (?, ?, ?, ?, ?, ?) returning id") {
            executeReturningInsert(sourceType.id, docType.id, className, baseDoc.identifier, embedJson, sourceLink).readOnce()!!["id"]
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

    private fun ClassDoc.asGithubLink(): String? {
        if (sourceType.githubSourceURL == null) return null

        val packageName = packageName.replace('.', '/')
        val topLevelClassName = className.substringBefore('.')
        return "${sourceType.githubSourceURL}$packageName/$topLevelClassName.java"
    }

    companion object {
        internal val GSON = GsonBuilder()
            .registerTypeAdapter(MessageEmbed::class.java, MessageEmbedAdapter)
            .create()
    }
}