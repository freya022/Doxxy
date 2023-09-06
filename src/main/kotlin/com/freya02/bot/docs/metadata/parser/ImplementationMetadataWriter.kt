package com.freya02.bot.docs.metadata.parser

import com.freya02.bot.docs.index.ReindexData
import com.freya02.bot.utils.createProfiler
import com.freya02.bot.utils.nextStep
import com.freya02.botcommands.api.core.db.Transaction
import com.freya02.docs.DocSourceType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.intellij.lang.annotations.Language
import org.slf4j.profiler.Profiler

internal class ImplementationMetadataWriter private constructor(
    private val sourceType: DocSourceType,
    private val reindexData: ReindexData,
    private val sourceRootMetadata: SourceRootMetadata
) {
    context(Transaction, Profiler)
    private suspend fun reindex() {
        @Language("PostgreSQL")
        val deleteStatements = listOf(
            "delete from implementation where (select source_id from class where id = class_id) = ?",
            "delete from subclass where (select source_id from class where id = subclass_id or id = superclass_id limit 1) = ?",
            "delete from method where (select source_id from class where id = class_id) = ?",
            "delete from class where source_id = ?"
        )
        nextStep("Delete old data") {
            preparedStatement("""
                alter table implementation disable trigger all;
                alter table class disable trigger all;
                alter table method disable trigger all;
                alter table subclass disable trigger all;
            """.trimIndent()) { executeUpdate() }

            deleteStatements.forEach { preparedStatement(it) { executeUpdate(sourceType.id) } }

            //Enable back after deleting as triggers are also FK checks
            preparedStatement("""
                alter table implementation enable trigger all;
                alter table class enable trigger all;
                alter table method enable trigger all;
                alter table subclass enable trigger all;
            """.trimIndent()) { executeUpdate() }
        }

        val classes = sourceRootMetadata.implementationMetadata.classes.values
        // First add the classes so we can reference them later
        val dbClasses: Map<ImplementationMetadata.Class, Int> = nextStep("Add classes") { addClasses(classes) }

        //Add subclass relations
        nextStep("Add subclasses") { addSubclasses(classes, dbClasses) }

        //Add methods
        val dbMethods: Map<ImplementationMetadata.Method, Int> = nextStep("Add methods") { addMethods(classes, dbClasses) }

        //Add implementations
        nextStep("Add implementations") { addImplementations(classes, dbClasses, dbMethods) }
    }

    context(Transaction)
    private suspend fun addClasses(classes: Collection<ImplementationMetadata.Class>): Map<ImplementationMetadata.Class, Int> {
        return classes.associateWith {
            preparedStatement(
                """
                    insert into class (source_id, class_type, package_name, class_name, source_link)
                    values (?, ?, ?, ?, ?)
                    returning id
                """.trimIndent()
            ) {
                executeQuery(sourceType.id, it.classType.id, it.packageName, it.name, reindexData.getClassSourceUrl(it))
                    .read().getInt(1)
            }
        }
    }

    context(Transaction)
    private suspend fun addSubclasses(
        classes: Collection<ImplementationMetadata.Class>,
        dbClasses: Map<ImplementationMetadata.Class, Int>
    ) {
        val subclasses = classes.flatMap { superclass ->
            superclass.subclasses.map { subclass ->
                listOf(dbClasses[superclass], dbClasses[subclass])
            }
        }

        withContext(Dispatchers.IO) {
            connection.copyFrom("subclass", subclasses)
        }
    }

    context(Transaction)
    private suspend fun addMethods(
        classes: Collection<ImplementationMetadata.Class>,
        dbClasses: Map<ImplementationMetadata.Class, Int>
    ): Map<ImplementationMetadata.Method, Int> {
        return classes.flatMap { it.declaredMethods.values }.associateWith { method ->
            preparedStatement(
                """
                    insert into method (class_id, method_type, name, signature, source_link)
                    values (?, ?, ?, ?, ?)
                    returning id
                """.trimIndent()
            ) {
                val methodSourceUrl = reindexData.getMethodSourceUrl(method)

                executeQuery(
                    dbClasses[method.owner],
                    method.type.id,
                    method.name,
                    method.signature,
                    methodSourceUrl
                ).read().getInt(1)
            }
        }
    }

    context(Transaction)
    private suspend fun addImplementations(
        classes: Collection<ImplementationMetadata.Class>,
        dbClasses: Map<ImplementationMetadata.Class, Int>,
        dbMethods: Map<ImplementationMetadata.Method, Int>
    ) {
        val implementations = classes.flatMap { clazz ->
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
                .flatMap insertLoop@{ (superMethod, implementations) ->
                    implementations.map {
                        listOf(dbClasses[clazz], dbMethods[superMethod], dbMethods[it])
                    }
                }
        }

        withContext(Dispatchers.IO) {
            connection.copyFrom("implementation", implementations)
        }
    }

    companion object {
        context(Transaction)
        suspend fun reindex(
            sourceType: DocSourceType,
            reindexData: ReindexData,
            sourceRootMetadata: SourceRootMetadata
        ) {
            createProfiler("ImplementationMetadataWriter") {
                ImplementationMetadataWriter(sourceType, reindexData, sourceRootMetadata).reindex()
            }
        }
    }
}