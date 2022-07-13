package com.freya02.bot.docs.index

import com.freya02.bot.db.DBAction
import com.freya02.bot.db.Database
import com.freya02.bot.docs.DocEmbeds.toEmbed
import com.freya02.botcommands.api.Logging
import com.freya02.docs.ClassDocs
import com.freya02.docs.DocSourceType
import com.freya02.docs.DocsSession
import com.freya02.docs.data.BaseDoc
import com.freya02.docs.data.ClassDoc
import com.google.gson.GsonBuilder
import net.dv8tion.jda.api.entities.MessageEmbed
import java.sql.ResultSet

private val LOGGER = Logging.getLogger()
private val GSON = GsonBuilder()
    .registerTypeAdapter(MessageEmbed::class.java, MessageEmbedAdapter())
    .create()

internal class DocIndexWriter(private val database: Database, private val docsSession: DocsSession, private val sourceType: DocSourceType) {
    fun doReindex() {
        val updatedSource = ClassDocs.getUpdatedSource(sourceType)

        DBAction.of(database, "delete from doc where source_id = ?").use { it.executeUpdate(sourceType.id) }

        for ((className, classUrl) in updatedSource.getSimpleNameToUrlMap()) {
            try {
                val classDoc = docsSession.retrieveDoc(classUrl)

                if (classDoc == null) {
                    LOGGER.warn("Unable to get docs of '${className}' at '${classUrl}', javadoc version or source type may be incorrect")
                    continue
                }

                val classEmbed = toEmbed(classDoc).build()
                val classEmbedJson = GSON.toJson(classEmbed)

                val classDocId = insertDoc(null, DocType.CLASS, classDoc, classEmbedJson)
                insertSeeAlso(classDoc, classDocId)

                insertMethodDocs(classDoc, classDocId)
                insertFieldDocs(classDoc, classDocId)
            } catch (e: Exception) {
                throw RuntimeException("An exception occurred while reading the docs of '$className' at '$classUrl'", e)
            }
        }
    }

    private fun insertMethodDocs(classDoc: ClassDoc, classDocId: Int) {
        for (methodDoc in classDoc.getMethodDocs().values) {
            try {
                val methodEmbed = toEmbed(classDoc, methodDoc).build()
                val methodEmbedJson = GSON.toJson(methodEmbed)

                val methodDocId = insertDoc(classDocId, DocType.METHOD, methodDoc, methodEmbedJson)
                insertSeeAlso(methodDoc, methodDocId)
            } catch (e: Exception) {
                throw RuntimeException(
                    "An exception occurred while reading the docs of " + classDoc.className + "#" + methodDoc.simpleSignature,
                    e
                )
            }
        }
    }

    private fun insertFieldDocs(classDoc: ClassDoc, classDocId: Int) {
        for (fieldDoc in classDoc.getFieldDocs().values) {
            try {
                val fieldEmbed = toEmbed(classDoc, fieldDoc).build()
                val fieldEmbedJson = GSON.toJson(fieldEmbed)

                val fieldDocId = insertDoc(classDocId, DocType.FIELD, fieldDoc, fieldEmbedJson)
                insertSeeAlso(fieldDoc, fieldDocId)
            } catch (e: Exception) {
                throw RuntimeException(
                    "An exception occurred while reading the docs of " + classDoc.className + "#" + fieldDoc.simpleSignature,
                    e
                )
            }
        }
    }

    private fun insertDoc(
        classDocId: Int? = null,
        docType: DocType,
        baseDoc: BaseDoc,
        embedJson: String?
    ): Int = DBAction.of(
        database,
        "insert into doc (source_id, type, parent_id, name, embed) VALUES (?, ?, ?, ?, ?) returning id",
        "id"
    ).use { action ->
        action.executeUpdate(sourceType.id, docType.id, classDocId, baseDoc.asDBName, embedJson)
        return@use action.nextGeneratedRow().getInt("id")
    }

    private fun insertSeeAlso(baseDoc: BaseDoc, docId: Int) {
        baseDoc.seeAlso?.getReferences()?.forEach { seeAlsoReference ->
            DBAction.of(
                database,
                "insert into docseealsoreference (doc_id, text, link, target_type, full_signature) VALUES (?, ?, ?, ?, ?)"
            ).use { action ->
                action.executeUpdate(
                    docId,
                    seeAlsoReference.text,
                    seeAlsoReference.link,
                    seeAlsoReference.targetType.id,
                    seeAlsoReference.fullSignature
                )
            }
        }
    }

    private fun DBAction.nextGeneratedRow(): ResultSet {
        if (!preparedStatement.generatedKeys.next()) {
            throw IllegalStateException("No generated keys")
        }

        return preparedStatement.generatedKeys
    }
}