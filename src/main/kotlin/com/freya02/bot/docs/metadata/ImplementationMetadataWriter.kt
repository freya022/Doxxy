package com.freya02.bot.docs.metadata

import com.freya02.bot.docs.index.ReindexData
import com.freya02.botcommands.api.core.db.Transaction
import com.freya02.docs.DocSourceType

internal class ImplementationMetadataWriter private constructor(
    private val sourceType: DocSourceType,
    private val reindexData: ReindexData,
    private val sourceRootMetadata: SourceRootMetadata
) {
    context(Transaction)
    private suspend fun reindex() {
        preparedStatement("delete from class where source_id = ?") { executeUpdate(sourceType.id) }

        // First add the classes so we can reference them later
        val subclasses = sourceRootMetadata.implementationMetadata.classes.values
        val dbClasses: Map<ImplementationMetadata.Class, Int> =
            subclasses.associateWith {
                preparedStatement(
                    """
                        insert into class (source_id, qualified_name, source_link)
                        values (?, ?, ?)
                        returning id
                    """.trimIndent()
                ) {
                    executeQuery(sourceType.id, it.qualifiedName, reindexData.getClassSourceUrl(it)).readOnce()!!
                        .getInt(1)
                }
            }

        //Add subclass relations
        subclasses.forEach { superclass ->
            superclass.subclasses.forEach { subclass ->
                preparedStatement(
                    """
                        insert into subclass (superclass_id, subclass_id) values (?, ?)
                    """.trimIndent()
                ) {
                    executeUpdate(dbClasses[superclass], dbClasses[subclass])
                }
            }
        }

        //Add methods
        val dbMethods: Map<ImplementationMetadata.Method, Int> = hashMapOf<ImplementationMetadata.Method, Int>().apply {
            subclasses.flatMap { it.declaredMethods.values }.forEach { method ->
                preparedStatement(
                    """
                        insert into method (class_id, name, signature, source_link)
                        values (?, ?, ?, ?)
                        returning id
                    """.trimIndent()
                ) {
                    val methodRange = method.range
                    val methodSourceUrl =
                        "${reindexData.getClassSourceUrl(method.owner)}#L${methodRange.first}-L${methodRange.last}"

                    this@apply[method] = executeQuery(
                        dbClasses[method.owner],
                        method.name,
                        method.declaration.signature,
                        methodSourceUrl
                    ).readOnce()!!.getInt(1)
                }
            }
        }

        //Add implementations
        subclasses.forEach { superclass ->
            //Find the implementations of that superclass, inside the subclasses
            superclass.methods
                .associateWith { it.implementations }
                .mapValues { (_, v) -> v.filter { superclass.subclasses.any { subclass -> subclass in it.owner.subclasses } } }
                .forEach { (superMethod, implementations) ->
                    implementations.forEach { implementation ->
                        preparedStatement(
                            """
                                insert into implementation (class_id, method_id, implementation_id)
                                values (?, ?, ?)
                            """.trimIndent()
                        ) {
                            executeUpdate(dbClasses[superclass], dbMethods[superMethod], dbMethods[implementation])
                        }
                    }
                }
        }
    }

    companion object {
        context(Transaction)
        suspend fun reindex(
            sourceType: DocSourceType,
            reindexData: ReindexData,
            sourceRootMetadata: SourceRootMetadata
        ) {
            ImplementationMetadataWriter(sourceType, reindexData, sourceRootMetadata).reindex()
        }
    }
}