package com.freya02.bot.docs.index

import com.freya02.bot.Main
import com.freya02.bot.db.Database
import com.freya02.bot.db.Transaction
import com.freya02.bot.docs.DocEmbeds.toEmbed
import com.freya02.botcommands.api.Logging
import com.freya02.docs.ClassDocs
import com.freya02.docs.DocSourceType
import com.freya02.docs.DocsSession
import com.freya02.docs.data.BaseDoc
import com.freya02.docs.data.ClassDoc
import com.github.javaparser.ast.NodeList
import com.github.javaparser.ast.body.ConstructorDeclaration
import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.nodeTypes.NodeWithParameters
import com.github.javaparser.ast.nodeTypes.NodeWithRange
import com.github.javaparser.ast.nodeTypes.NodeWithSimpleName
import com.github.javaparser.ast.visitor.VoidVisitorAdapter
import com.github.javaparser.utils.SourceRoot
import com.google.gson.GsonBuilder
import net.dv8tion.jda.api.entities.MessageEmbed

private val LOGGER = Logging.getLogger()

internal class DocIndexWriter(private val database_: Database, private val docsSession: DocsSession, private val sourceType: DocSourceType) {
    private val sourceRoot: SourceRoot?

    init {
        val docsFolderName = when (sourceType) {
            DocSourceType.JDA -> "JDA"
            DocSourceType.BOT_COMMANDS -> "BotCommands"
            else -> null
        }

        sourceRoot = when {
            docsFolderName != null -> SourceRoot(Main.JAVADOCS_PATH.resolve(docsFolderName))
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
                val sourceLink = run {
                    when (sourceType.githubSourceURL) {
                        null -> null
                        else -> sourceType.githubSourceURL + classDoc.packageName.replace('.', '/') + "/${classDoc.className}.java"
                    }
                }

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
                    else -> sourceRoot?.let { sourceRoot ->
//                        val docsMethodParams: List<Pair<String, String>> = when (methodDoc.methodParameters) {
//                            null -> emptyList()
//                            else -> {
//                                methodDoc
//                                    .methodParameters
//                                    .substring(1, methodDoc.methodParameters.length - 1)
//                                    .split(",").map(String::trim).map { parameterStr ->
//                                        val match =
//                                            "(\\S+\\s*)*?(\\S+)\\s+(\\S+)".toRegex().matchEntire(parameterStr)
//                                                ?: return@let null
//
//                                        match.groups[2]!!.value
//                                            .replace("...", "[]")
//                                            .let { originalStr ->
//                                                "[\\w.]+".toRegex().replace(originalStr) { result ->
//                                                    result.value.split(".").last()
//                                                }
//                                            } to match.groups[3]!!.value
//                                    }
//                            }
//                        }

                        val docsParametersString = methodDoc.methodParameters
                            ?.drop(1)
                            ?.dropLast(1)
                            ?.replace("@\\w+ ".toRegex(), "")
//                            ?.split(",".toRegex())
//                            ?.joinToString(",") {
//                                it.substringBefore(' ').replace("(\\w+)\\.(?=\\w)".toRegex(), "") + " " + it.substringAfter(' ')
//                            }
                            ?: ""

                        val compilationUnit = sourceRoot.parse(
                            methodDoc.classDocs.packageName,
                            "${methodDoc.classDocs.className}.java"
                        )

                        var range: IntRange? = null
                        compilationUnit.accept(object : VoidVisitorAdapter<Void>() {
                            override fun visit(n: ConstructorDeclaration, arg: Void?) {
                                if (range != null) return
                                handleDeclaration(n)
                                super.visit(n, arg)
                            }

                            override fun visit(n: MethodDeclaration, arg: Void?) {
                                if (range != null) return
                                handleDeclaration(n)
                                super.visit(n, arg)
                            }

                            private fun <T> handleDeclaration(n: T)
                                    where T : NodeWithSimpleName<T>,
                                          T : NodeWithParameters<T>,
                                          T : NodeWithRange<*> {
                                if (n.getName().asString() == methodDoc.methodName) {
                                    val parametersString = n.getParameters()
                                        .onEach { it.annotations = NodeList.nodeList() }
                                        .joinToString()

                                    LOGGER.debug(docsParametersString)
                                    LOGGER.debug(parametersString)

                                    if (parametersString == docsParametersString) {
                                        range = n.begin.get().line..n.end.get().line
                                    }
                                }
                            }
                        }, null)

//                        val classInfo = sourceRoot["${methodDoc.classDocs.packageName}.${methodDoc.classDocs.className}"]
//
//                        methodLoop@ for (methodInfo in classInfo.getDeclaredMethodInfo(methodDoc.methodName)) {
//                            for ((i, parameterInfo) in methodInfo.parameterInfo.withIndex()) {
//                                val descriptor = parameterInfo.typeSignatureOrTypeDescriptor
//
//                                if (descriptor.toStringWithSimpleNames() != docsMethodParams[i].first) {
//                                    continue@methodLoop
//                                }
//                            }
//
//                            return@let methodInfo.minLineNum..methodInfo.maxLineNum
//                        }

                        if (range != null) return@let range

                        LOGGER.warn("Method not found: ${methodDoc.methodSignature}")

                        null
                    }
                }

                val methodLink = when (methodRange) {
                    null -> null
                    else -> "$sourceLink#L${methodRange.first}-L${methodRange.last}"
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

    companion object {
        internal val GSON = GsonBuilder()
            .registerTypeAdapter(MessageEmbed::class.java, MessageEmbedAdapter)
            .create()
    }
}