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
        val classes = sourceRootMetadata.implementationMetadata.classes.values
        val dbClasses: Map<ImplementationMetadata.Class, Int> =
            classes.associateWith {
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
        classes.forEach { superclass ->
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
            classes.flatMap { it.declaredMethods.values }.forEach { method ->
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
        classes.forEach { clazz ->
            //Find the implementations of that class's methods, inside the subclasses
            clazz.methods
                //This eliminates overridden methods to only keep the top most declaration
                // This works only because top most declarations are always above in the list,
                // due to how ImplementationMetadata.Class#methods work
                .distinctBy { it.descriptor }
                .associateWith {
                    it.implementations.filter { implementation ->
                        //Keep implementations that comes from subclasses
                        implementation.owner.subclasses.containsAny(clazz.subclasses)
                    }
                }
                .forEach { (superMethod, implementations) ->
                    implementations.forEach { implementation ->
                        preparedStatement(
                            """
                                insert into implementation (class_id, method_id, implementation_id)
                                values (?, ?, ?)
                            """.trimIndent()
                        ) {
                            executeUpdate(dbClasses[clazz], dbMethods[superMethod], dbMethods[implementation])
                        }
                    }
                }
        }
    }

    private fun <T> Collection<T>.containsAny(col: Collection<T>) =
        col.any { item -> item in this }

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