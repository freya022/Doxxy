package dev.freya02.doxxy.bot.docs.metadata.parser

import dev.freya02.doxxy.bot.docs.DocSourceType
import dev.freya02.doxxy.bot.docs.index.ReindexData
import dev.freya02.doxxy.bot.utils.createProfiler
import dev.freya02.doxxy.bot.utils.nextStep
import io.github.freya022.botcommands.api.core.db.Transaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.intellij.lang.annotations.Language
import org.postgresql.copy.CopyManager
import org.postgresql.core.BaseConnection
import org.slf4j.profiler.Profiler
import java.sql.Connection

internal class ImplementationMetadataWriter private constructor(
    private val sourceType: DocSourceType,
    private val reindexData: ReindexData,
    private val sourceMetadata: SourceMetadata
) {
    context(transaction: Transaction, profiler: Profiler)
    private suspend fun reindex() = withContext(Dispatchers.IO) {
        @Language("PostgreSQL")
        val deleteStatements = listOf(
            "delete from implementation where (select source_id from class where id = class_id) = ?",
            "delete from subclass where (select source_id from class where id = subclass_id or id = superclass_id limit 1) = ?",
            "delete from method where (select source_id from class where id = class_id) = ?",
            "delete from class where source_id = ?"
        )
        profiler.nextStep("Delete old data") {
            transaction.preparedStatement("""
                alter table implementation disable trigger all;
                alter table class disable trigger all;
                alter table method disable trigger all;
                alter table subclass disable trigger all;
            """.trimIndent()) { executeUpdate() }

            deleteStatements.forEach { transaction.preparedStatement(it) { executeUpdate(sourceType.id) } }

            //Enable back after deleting as triggers are also FK checks
            transaction.preparedStatement("""
                alter table implementation enable trigger all;
                alter table class enable trigger all;
                alter table method enable trigger all;
                alter table subclass enable trigger all;
            """.trimIndent()) { executeUpdate() }
        }

        val classes = sourceMetadata.implementationMetadata.classes.values
        // First add the classes so we can reference them later
        val dbClasses: Map<ImplementationMetadata.Class, Int> = profiler.nextStep("Add classes") { addClasses(classes) }

        //Add subclass relations
        profiler.nextStep("Add subclasses") { addSubclasses(classes, dbClasses) }

        //Add methods
        val dbMethods: Map<ImplementationMetadata.Method, Int> = profiler.nextStep("Add methods") { addMethods(classes, dbClasses) }

        //Add implementations
        profiler.nextStep("Add implementations") { addImplementations(classes, dbClasses, dbMethods) }
    }

    context(transaction: Transaction)
    private suspend fun addClasses(classes: Collection<ImplementationMetadata.Class>): Map<ImplementationMetadata.Class, Int> {
        return transaction.preparedStatement(
            """
                insert into class (source_id, class_type, package_name, class_name, source_link)
                values (?, ?, ?, ?, ?)
                returning id
            """.trimIndent(),
            columnNames = arrayOf("id")
        ) {
            classes.forEach {
                setParameters(arrayOf<Any?>(
                    sourceType.id,
                    it.classType.id,
                    it.packageName,
                    it.name,
                    reindexData.getClassSourceUrl(it)
                ))

                addBatch()
            }

            executeBatch_()

            val generatedKeys = generatedKeys
            classes.associateWith { generatedKeys.read().getInt(1) }
        }
    }

    context(transaction: Transaction)
    private fun addSubclasses(
        classes: Collection<ImplementationMetadata.Class>,
        dbClasses: Map<ImplementationMetadata.Class, Int>
    ) {
        val subclasses = classes.flatMap { superclass ->
            superclass.subclasses.map { subclass ->
                listOf(dbClasses[superclass], dbClasses[subclass])
            }
        }

        transaction.connection.copyFrom("subclass", subclasses)
    }

    context(transaction: Transaction)
    private suspend fun addMethods(
        classes: Collection<ImplementationMetadata.Class>,
        dbClasses: Map<ImplementationMetadata.Class, Int>
    ): Map<ImplementationMetadata.Method, Int> {
        return transaction.preparedStatement(
            """
                insert into method (class_id, method_type, name, signature, source_link)
                values (?, ?, ?, ?, ?)
                returning id
            """.trimIndent(),
            columnNames = arrayOf("id")
        ) {
            val methods = classes.flatMap { it.declaredMethods.values }

            methods.forEach { method ->
                val methodSourceUrl = reindexData.getMethodSourceUrl(method)

                setParameters(arrayOf<Any?>(
                    dbClasses[method.owner],
                    method.type.id,
                    method.name,
                    method.signature,
                    methodSourceUrl
                ))

                addBatch()
            }

            executeBatch_()

            val generatedKeys = generatedKeys
            methods.associateWith { generatedKeys.read().getInt(1) }
        }
    }

    context(transaction: Transaction)
    private fun addImplementations(
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

        transaction.connection.copyFrom("implementation", implementations)
    }

    companion object {
        context(_: Transaction)
        suspend fun reindex(
            sourceType: DocSourceType,
            reindexData: ReindexData,
            sourceMetadata: SourceMetadata
        ) {
            createProfiler("ImplementationMetadataWriter") {
                ImplementationMetadataWriter(sourceType, reindexData, sourceMetadata).reindex()
            }
        }
    }
}

private fun Connection.copyFrom(
    @Language("PostgreSQL", prefix = "copy ", suffix = " from stdin delimiter ','") table: String,
    list: Collection<Collection<Any?>>,
) {
    CopyManager(unwrap(BaseConnection::class.java))
        .copyIn("copy $table from stdin delimiter ','", list.joinToString("\n") { it.joinToString(",") }.byteInputStream())
}