package com.freya02.bot.examples

import com.freya02.docs.DocSourceType
import io.github.freya022.botcommands.api.core.db.Database
import io.github.freya022.botcommands.api.core.db.preparedStatement
import io.github.freya022.botcommands.api.core.service.annotations.BService
import io.github.freya022.botcommands.api.core.utils.enumMapOf
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import java.util.*

@JvmInline
@Serializable
private value class SimpleClassName(val str: String) {
    init {
        if ('#' in str || '.' in str) {
            throw BadRequestException("Invalid simple class name: $str")
        }
    }

    override fun toString(): String = str
}

@JvmInline
@Serializable
private value class PartialIdentifier(val str: String) {
    val type get() = when {
        '(' in str -> Type.FULL_METHOD
        str.all { !it.isLetter() || it.isUpperCase() } -> Type.FIELD
        else -> Type.OVERLOADS
    }

    enum class Type {
        FULL_METHOD,
        OVERLOADS,
        FIELD
    }

    init {
        if ('#' in str) {
            throw BadRequestException("Invalid partial identifier: $str")
        }
    }

    override fun toString(): String = str
}

@JvmInline
@Serializable
private value class QualifiedPartialIdentifier(val str: String) {
    val className get() = SimpleClassName(str.substringBefore('#'))
    val identifier get() = PartialIdentifier(str.substringAfter('#'))

    init {
        if ('#' !in str) {
            throw BadRequestException("Invalid qualified partial identifier: $str")
        }
    }

    override fun toString(): String = str
}

@BService
class ExampleTargetsController(private val database: Database) {
    @Serializable
    private class RequestedTargets(
        val sourceTypeToSimpleClassNames: Map<DocSourceType, Set<SimpleClassName>>,
        val sourceTypeToQualifiedPartialIdentifiers: Map<DocSourceType, Set<QualifiedPartialIdentifier>>
    )

    @Serializable
    private class MissingTargets(
        val sourceTypeToSimpleClassNames: Map<DocSourceType, Set<SimpleClassName>>,
        val sourceTypeToPartialIdentifiers: Map<DocSourceType, Set<QualifiedPartialIdentifier>>
    )

    init {
        embeddedServer(Netty, port = 63156) {
            install(ContentNegotiation) {
                json()
            }

            routing {
                checkTargets()
            }
        }.start(wait = false)
    }

    /**
     * Checks if the simple class names, and the qualified (potentially partial) identifiers exists.
     *
     * Caller does not need to know the full identifiers of the partial identifiers, only to know they exist.
     *
     * Returns an object with a list of missing classes, and a list of unmatched identifiers.
     */
    private fun Routing.checkTargets() {
        post("/examples/targets/check") {
            val targets: RequestedTargets = call.receive()

            val existingClassName = findClassNames(targets.sourceTypeToSimpleClassNames)
            val existingQualifiedPartialIdentifiers = findFullSignatures(targets.sourceTypeToQualifiedPartialIdentifiers)

            val missingClasses = existingClassName.mapValues { (sourceType, existingClassNames) ->
                val requestedClassNames = targets.sourceTypeToSimpleClassNames[sourceType]!!
                requestedClassNames - existingClassNames
            }

            val missingPartialIdentifiers = existingQualifiedPartialIdentifiers.mapValues { (sourceType, existingQualifiedPartialIdentifiers) ->
                val requestedQualifiedPartialIdentifiers = targets.sourceTypeToQualifiedPartialIdentifiers[sourceType]!!
                requestedQualifiedPartialIdentifiers - existingQualifiedPartialIdentifiers
            }

            call.respond(MissingTargets(missingClasses, missingPartialIdentifiers))
        }
    }

    private suspend fun findClassNames(classes: Map<DocSourceType, Set<SimpleClassName>>): Map<DocSourceType, Set<SimpleClassName>> {
        return classes.mapValuesTo(enumMapOf()) { (sourceType, targetClasses) ->
            database.preparedStatement(
                """
                    select d.classname
                    from doc d
                    where d.source_id = ?
                      and d.classname = any (?)
                """.trimIndent(), readOnly = true
            ) {
                executeQuery(sourceType.id, targetClasses.map { it.str }.toTypedArray()).mapTo(hashSetOf()) { it["classname"] }
            }
        }
    }

    private suspend fun findFullSignatures(sourceMap: Map<DocSourceType, Set<QualifiedPartialIdentifier>>): Map<DocSourceType, Set<QualifiedPartialIdentifier>> {
        return sourceMap.mapValuesTo(enumMapOf()) { (sourceType, targetReferences) ->
            val conditions = StringJoiner(") or (", "(", ")")
            val conditionValues = arrayListOf<String>()

            // Find all members (fields/methods)
            targetReferences
                .groupBy { it.className }
                .forEach { (className, qualifiedPartialIdentifiers) ->
                    val memberConditions = StringJoiner(" or ")
                    val memberConditionValues = arrayListOf<String>()
                    qualifiedPartialIdentifiers.map { it.identifier }.forEach { partialIdentifier ->
                        when (partialIdentifier.type) {
                            PartialIdentifier.Type.FULL_METHOD -> memberConditions.add("d.identifier = ?")
                            PartialIdentifier.Type.OVERLOADS -> memberConditions.add("d.identifier like ? || '%'")
                            PartialIdentifier.Type.FIELD -> memberConditions.add("d.identifier = ?")
                        }
                        memberConditionValues += partialIdentifier.str
                    }
                    conditions.add("d.classname = ? and ($memberConditions)")
                    conditionValues += className.str
                    conditionValues += memberConditionValues
                }

            database.preparedStatement(
                """
                    select d.classname || '#' || split_part(d.identifier, '(', 1) as qualified_partial_identifier
                    from doc d
                    where d.source_id = ?
                      and d.type != 1
                      and ($conditions)
                """.trimIndent()
            ) {
                executeQuery(sourceType.id, *conditionValues.toTypedArray()).mapTo(hashSetOf()) {
                    QualifiedPartialIdentifier(it.getString("qualified_partial_identifier"))
                }
            }
        }
    }
}