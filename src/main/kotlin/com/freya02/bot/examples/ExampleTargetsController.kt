package com.freya02.bot.examples

import com.freya02.docs.DocSourceType
import io.github.freya022.botcommands.api.core.db.Database
import io.github.freya022.botcommands.api.core.db.preparedStatement
import io.github.freya022.botcommands.api.core.service.annotations.BService
import io.github.freya022.botcommands.api.core.utils.enumMapOf
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.util.*

@JvmInline
private value class SimpleClassName(val str: String) {
    init {
        if ('#' in str || '.' in str) {
            throw BadRequestException("Invalid simple class name: $str")
        }
    }

    override fun toString(): String = str
}
@JvmInline
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
@JvmInline
private value class FullQualifiedIdentifier(val str: String) {
    val isMethod get() = '(' in str

    fun startsWith(qualifiedPartialIdentifier: QualifiedPartialIdentifier): Boolean =
        str.startsWith(qualifiedPartialIdentifier.str)

    fun isSameAs(qualifiedPartialIdentifier: QualifiedPartialIdentifier): Boolean =
        str == qualifiedPartialIdentifier.str

    override fun toString(): String = str
}

@BService
class ExampleTargetsController(private val database: Database) {
    private class RequestedTargets(
        val sourceTypeToSimpleClassNames: EnumMap<DocSourceType, List<SimpleClassName>>,
        val sourceTypeToQualifiedPartialIdentifiers: EnumMap<DocSourceType, List<QualifiedPartialIdentifier>>
    )
    private class MappedTargets(
        val sourceTypeToSimpleClassNames: Map<DocSourceType, List<SimpleClassName>>,
        val sourceTypeToMappedIdentifiers: Map<DocSourceType, Map<QualifiedPartialIdentifier, List<FullQualifiedIdentifier>>>
    )
    private enum class ReferenceType {
        METHOD,
        FIELD
    }

    init {
        embeddedServer(Netty, port = 63156) {
            routing {
                resolveTargets()
            }
        }.start(wait = false)
    }

    // TODO this can probably be simplified even more if we only pass partial identifiers (i.e., no full method, no fields)
    //   This will need to be checked of course, and also requires another step where all (full) identifiers are checked against the database
    // Find all targets the best as we can,
    // the backend will then check that every target has been assigned actual "identifiers"
    private fun Routing.resolveTargets() {
        post("/examples/targets") {
            val targets: RequestedTargets = call.receive()

            val mappedClasses = findClassNames(targets.sourceTypeToSimpleClassNames)
            val fullSignatures = findFullSignatures(targets.sourceTypeToQualifiedPartialIdentifiers).mapValuesTo(enumMapOf()) {
                it.value.groupByTo(enumMapOf()) { target ->
                    if (target.isMethod) {
                        ReferenceType.METHOD
                    } else {
                        ReferenceType.FIELD
                    }
                }
            }

            // Map qualified partial identifiers to their list of fully qualified identifiers
            val mappedMembers: Map<DocSourceType, Map<QualifiedPartialIdentifier, List<FullQualifiedIdentifier>>> =
                // For each source type
                targets.sourceTypeToQualifiedPartialIdentifiers.mapValuesTo(enumMapOf()) { (sourceType, targetReferences) ->
                    val fullQualifiedIdentifiersByReferenceType = fullSignatures[sourceType] ?: return@mapValuesTo emptyMap()
                    // Associate qualified partial identifiers (HTTP input)
                    // to their list of fully qualified identifiers
                    targetReferences.associateWith { qualifiedPartialIdentifier ->
                        val partialIdentifier = qualifiedPartialIdentifier.identifier
                        val fullQualifiedIdentifiers = fullQualifiedIdentifiersByReferenceType[partialIdentifier.referenceType]
                            ?: return@associateWith emptyList()

                        when (partialIdentifier.type) {
                            // Qualified partial identifiers may only specify the method name,
                            // in which case we take every method with that name (i.e., all overloads)
                            PartialIdentifier.Type.OVERLOADS -> fullQualifiedIdentifiers.filter { it.startsWith(qualifiedPartialIdentifier) }
                            PartialIdentifier.Type.FULL_METHOD, PartialIdentifier.Type.FIELD ->
                                fullQualifiedIdentifiers.filter { it.isSameAs(qualifiedPartialIdentifier) }
                        }
                    }
                }

            call.respond(MappedTargets(mappedClasses, mappedMembers))
        }
    }

    private val PartialIdentifier.referenceType: ReferenceType
        get() = when (type) {
            PartialIdentifier.Type.FULL_METHOD, PartialIdentifier.Type.OVERLOADS -> ReferenceType.METHOD
            PartialIdentifier.Type.FIELD -> ReferenceType.FIELD
        }

    private suspend fun findClassNames(classes: Map<DocSourceType, List<SimpleClassName>>): Map<DocSourceType, List<SimpleClassName>> {
        return classes.mapValuesTo(enumMapOf()) { (sourceType, targetClasses) ->
            database.preparedStatement(
                """
                    select d.classname
                    from doc d
                    where d.source_id = ?
                      and d.classname = any (?)
                """.trimIndent(), readOnly = true
            ) {
                executeQuery(sourceType.id, targetClasses.toTypedArray()).map { it["classname"] }
            }
        }
    }

    private suspend fun findFullSignatures(sourceMap: Map<DocSourceType, List<QualifiedPartialIdentifier>>): Map<DocSourceType, Set<FullQualifiedIdentifier>> {
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
                    select d.classname, d.identifier
                    from doc d
                    where d.source_id = ? and d.type != 1 and ($conditions)
                """.trimIndent()
            ) {
                executeQuery(sourceType.id, *conditionValues.toTypedArray()).mapTo(hashSetOf()) {
                    val className = it.getString("classname")
                    val identifier = it.getString("identifier")

                    FullQualifiedIdentifier("$className#$identifier")
                }
            }
        }
    }
}