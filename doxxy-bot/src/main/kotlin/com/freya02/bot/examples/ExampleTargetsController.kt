package com.freya02.bot.examples

import com.freya02.bot.switches.RequiresBackend
import com.freya02.docs.DocSourceType.Companion.toDocSourceType
import io.github.freya022.botcommands.api.core.db.Database
import io.github.freya022.botcommands.api.core.db.preparedStatement
import io.github.freya022.botcommands.api.core.service.annotations.BService
import io.github.freya022.botcommands.api.core.utils.enumMapOf
import io.github.freya022.botcommands.api.core.utils.namedDefaultScope
import io.github.freya022.doxxy.common.DocumentedExampleLibrary
import io.github.freya022.doxxy.common.PartialIdentifier
import io.github.freya022.doxxy.common.QualifiedPartialIdentifier
import io.github.freya022.doxxy.common.SimpleClassName
import io.github.freya022.doxxy.common.dto.MissingTargetsDTO
import io.github.freya022.doxxy.common.dto.RequestedTargetsDTO
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.util.*

@RequiresBackend
@BService
class ExampleTargetsController(private val database: Database) {
    private val serverScope = namedDefaultScope("Example targets server", 1)

    init {
        serverScope.embeddedServer(Netty, port = 63156) {
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
            val targets: RequestedTargetsDTO = call.receive()

            val existingClassName = findClassNames(targets.sourceTypeToSimpleClassNames)
            val existingQualifiedPartialIdentifiers = findFullSignatures(targets.sourceTypeToQualifiedPartialIdentifiers)

            val missingClasses = existingClassName.mapValues { (documentedExampleLibrary, existingClassNames) ->
                val requestedClassNames = targets.sourceTypeToSimpleClassNames[documentedExampleLibrary]!!
                requestedClassNames - existingClassNames
            }

            val missingPartialIdentifiers = existingQualifiedPartialIdentifiers.mapValues { (documentedExampleLibrary, existingQualifiedPartialIdentifiers) ->
                val requestedQualifiedPartialIdentifiers = targets.sourceTypeToQualifiedPartialIdentifiers[documentedExampleLibrary]!!
                requestedQualifiedPartialIdentifiers - existingQualifiedPartialIdentifiers
            }

            call.respond(MissingTargetsDTO(missingClasses, missingPartialIdentifiers))
        }
    }

    private suspend fun findClassNames(classes: Map<DocumentedExampleLibrary, Set<SimpleClassName>>): Map<DocumentedExampleLibrary, Set<SimpleClassName>> {
        return classes.mapValuesTo(enumMapOf()) { (documentedExampleLibrary, targetClasses) ->
            database.preparedStatement(
                """
                    select d.classname
                    from doc d
                    where d.source_id = ?
                      and d.classname = any (?)
                """.trimIndent(), readOnly = true
            ) {
                executeQuery(documentedExampleLibrary.toDocSourceType().id, targetClasses.map { it.str }.toTypedArray()).mapTo(hashSetOf()) { SimpleClassName(it["classname"]) }
            }
        }
    }

    private suspend fun findFullSignatures(sourceMap: Map<DocumentedExampleLibrary, Set<QualifiedPartialIdentifier>>): Map<DocumentedExampleLibrary, Set<QualifiedPartialIdentifier>> {
        return sourceMap.mapValuesTo(enumMapOf()) { (documentedExampleLibrary, targetReferences) ->
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
                executeQuery(documentedExampleLibrary.toDocSourceType().id, *conditionValues.toTypedArray()).mapTo(hashSetOf()) {
                    QualifiedPartialIdentifier(it.getString("qualified_partial_identifier"))
                }
            }
        }
    }
}