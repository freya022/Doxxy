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
import net.dv8tion.jda.api.utils.data.DataObject

private val logger = KotlinLogging.logger { }
private val annotationRegex: Regex = "@\\w+ ".toRegex()

internal class DocIndexWriter(
    private val database: Database,
    private val sourceType: DocSourceType,
    private val reindexData: ReindexData
) {
    private val sourceMetadata: SourceMetadata? = sourceType.sourceDirectoryPath?.let { SourceMetadata(it) }

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

            preparedStatement("delete from doc where source_id = ?") {
                executeUpdate(sourceType.id)
            }

            javadocModuleSession
                .classesAsFlow()
                .flowOn(Dispatchers.IO.limitedParallelism(parallelism = 8, name = "Document fetch"))
                .buffer()
                .collect { javadocClass ->
                    try {
                        val classEmbed = toEmbed(javadocClass)
                        val classEmbedJson = classEmbed.toData()
                        val sourceLink = reindexData.getClassSourceUrlOrNull(javadocClass)

                        val classDocId = insertDoc(DocType.CLASS, javadocClass.className, javadocClass, classEmbedJson, sourceLink)
                        insertSeeAlso(javadocClass, classDocId)

                        insertMethodDocs(javadocClass, sourceLink)
                        insertFieldDocs(javadocClass, sourceLink)
                    } catch (e: Exception) {
                        throw RuntimeException("An exception occurred while reading the docs of '${javadocClass.sourceURL}'", e)
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
    private suspend fun insertMethodDocs(clazz: JavadocClass, sourceLink: String?) {
        for (method in clazz.methods.values) {
            try {
                val methodEmbed = toEmbed(clazz, method)
                val methodEmbedJson = methodEmbed.toData()

                val methodLink: String? = run {
                    if (sourceLink == null || sourceMetadata == null) return@run null
                    if (method.isBuiltInEnumMethod()) return@run null

                    val methodRange: IntRange? = sourceMetadata.getMethodRange(method)
                    if (methodRange == null) {
                        logger.warn { "Method not found: ${method.methodSignature}" }
                        return@run null
                    }

                    // This is different from `sourceLink` as it could come from a superclass
                    val methodClassSourceLink = reindexData.getClassSourceUrlOrNull(method.declaringClass)
                    "$methodClassSourceLink#L${methodRange.first}-L${methodRange.last}"
                }

                val methodId = insertDoc(DocType.METHOD, clazz.className, method, methodEmbedJson, methodLink)
                insertSeeAlso(method, methodId)
            } catch (e: Exception) {
                throw RuntimeException(
                    "An exception occurred while reading the docs of " + clazz.className + "#" + method.simpleSignature,
                    e
                )
            }
        }
    }

    context(_: Transaction)
    private suspend fun insertFieldDocs(clazz: JavadocClass, sourceLink: String?) {
        for (field in clazz.fields.values) {
            try {
                val fieldEmbed = toEmbed(clazz, field)
                val fieldEmbedJson = fieldEmbed.toData()

                val fieldLink: String? = run {
                    if (sourceLink == null) return@run null
                    if (sourceMetadata == null) return@run null

                    val fieldRange: IntRange? = sourceMetadata.getFieldRange(field)
                    if (fieldRange == null) {
                        logger.warn { "Field not found: ${field.declaringClass.className}#${field.fieldName}" }
                        return@run null
                    }

                    // This is different from `sourceLink` as it could come from a superclass
                    val fieldClassSourceLink = reindexData.getClassSourceUrlOrNull(field.declaringClass)
                    "$fieldClassSourceLink#L${fieldRange.first}-L${fieldRange.last}"
                }

                val fieldId = insertDoc(DocType.FIELD, clazz.className, field, fieldEmbedJson, fieldLink)
                insertSeeAlso(field, fieldId)
            } catch (e: Exception) {
                throw RuntimeException(
                    "An exception occurred while reading the docs of " + clazz.className + "#" + field.simpleSignature,
                    e
                )
            }
        }
    }

    context(transaction: Transaction)
    private suspend fun insertDoc(
        docType: DocType,
        className: String,
        javadoc: AbstractJavadoc,
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
                javadoc.identifier,
                javadoc.identifierNoArgs,
                javadoc.humanIdentifier,
                javadoc.toHumanClassIdentifier(className),
                javadoc.returnTypeNoAnnotations,
                embedJson.toString(),
                javadoc.onlineURL,
                sourceLink
            ).read()["id"]
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
}

private fun JavadocMethod.isBuiltInEnumMethod(): Boolean {
    // Technically this isn't entirely accurate, but who leaves empty enums?
    if (declaringClass.enumConstants.isEmpty()) return false

    // values()
    if (methodName == "values" &&
        (methodParameters?.parameters?.size ?: 0) == 0
    ) return true

    // valueOf(java.lang.String)
    if (methodName == "valueOf" &&
        methodParameters?.parameters?.size == 1 &&
        methodParameters!!.parameters[0].type == "java.lang.String"
    ) return true

    return false
}

private fun SourceMetadata.getMethodRange(method: JavadocMethod): IntRange? {
    val docsParametersString = method.methodParameters
        ?.originalText
        ?.drop(1)
        ?.dropLast(1)
        ?.replace(annotationRegex, "")
        ?: ""

    if (docsParametersString.isEmpty() && method.classDetailType == ClassDetailType.CONSTRUCTOR) {
        return null // TODO handle constructors and remove
    } else if (docsParametersString.isEmpty() && method.classDetailType == ClassDetailType.ANNOTATION_ELEMENT) {
        return null // TODO handle annotation members and remove
    } else if (docsParametersString.contains("net.dv8tion.jda.internal")) {
        return null // TODO fix MethodDocParameter#type then remove
    } else if (docsParametersString.contains("okhttp3")) {
        return null // TODO fix MethodDocParameter#type then remove
    } else if (docsParametersString.contains("gnu.")) {
        return null // TODO fix MethodDocParameter#type then remove
    }

    return getMethodsParameters(method.declaringClass.classNameFqcn, method.methodName)
        .find { it.parametersString == docsParametersString }
        ?.methodRange
}

private fun SourceMetadata.getFieldRange(field: JavadocField): IntRange? {
    return getFieldMetadata(field.declaringClass.classNameFqcn, field.fieldName)?.fieldRange
}
