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

        val classes = sourceRootMetadata.implementationMetadata.classes.values
        // First add the classes so we can reference them later
        val dbClasses: Map<ImplementationMetadata.Class, Int> = addClasses(classes)

        //Add subclass relations
        addSubclasses(classes, dbClasses)

        //Add methods
        val dbMethods: Map<ImplementationMetadata.Method, Int> = addMethods(classes, dbClasses)

        //Add implementations
        addImplementations(classes, dbClasses, dbMethods)
    }

    private suspend fun Transaction.addClasses(classes: Collection<ImplementationMetadata.Class>): Map<ImplementationMetadata.Class, Int> {
        return classes.associateWith {
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
    }

    private suspend fun Transaction.addSubclasses(
        classes: Collection<ImplementationMetadata.Class>,
        dbClasses: Map<ImplementationMetadata.Class, Int>
    ) {
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
    }

    private suspend fun Transaction.addMethods(
        classes: Collection<ImplementationMetadata.Class>,
        dbClasses: Map<ImplementationMetadata.Class, Int>
    ): Map<ImplementationMetadata.Method, Int> {
        return classes.flatMap { it.declaredMethods.values }.associateWith { method ->
            preparedStatement(
                """
                    insert into method (class_id, name, signature, source_link)
                    values (?, ?, ?, ?)
                    returning id
                """.trimIndent()
            ) {
                val methodSourceUrl = reindexData.getMethodSourceUrl(method)

                executeQuery(
                    dbClasses[method.owner],
                    method.name,
                    method.declaration.signature,
                    methodSourceUrl
                ).readOnce()!!.getInt(1)
            }
        }
    }

    private suspend fun Transaction.addImplementations(
        classes: Collection<ImplementationMetadata.Class>,
        dbClasses: Map<ImplementationMetadata.Class, Int>,
        dbMethods: Map<ImplementationMetadata.Method, Int>
    ) {
        classes.forEach { clazz ->
            //Find the implementations of that class's methods, keep only relevant implementations
            clazz.methods
                //This eliminates overridden methods to only keep the top most declaration
                // This works only because top most declarations are always above in the list,
                // due to how ImplementationMetadata.Class#methods work
                .distinctBy { it.signature }
                .associateWith {
                    it.implementations.filter { implementation ->
                        //Keep implementations that comes from a superclass of our own subclasses
                        // If this is a VoiceChannelManager,
                        // we want to keep implementations from VoiceChannelManagerImpl and it's subclasses
                        clazz.subclasses.any { sub -> sub.isSubclassOf(implementation.owner) }
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