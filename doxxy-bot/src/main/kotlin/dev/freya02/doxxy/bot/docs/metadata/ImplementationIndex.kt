package dev.freya02.doxxy.bot.docs.metadata

import dev.freya02.doxxy.bot.docs.index.DocIndex
import dev.freya02.doxxy.bot.docs.metadata.parser.FullSimpleClassName
import dev.freya02.doxxy.docs.DocSourceType
import io.github.freya022.botcommands.api.core.db.DBResult
import io.github.freya022.botcommands.api.core.db.Database
import io.github.freya022.botcommands.api.core.db.preparedStatement

class ImplementationIndex(val docIndex: DocIndex, private val database: Database) {
    val sourceType: DocSourceType
        get() = docIndex.sourceType

    inner class Class(val className: String, val classType: ClassType, val sourceLink: String) {
        val index: ImplementationIndex
            get() = this@ImplementationIndex

        constructor(result: DBResult) : this(
            result["class_name"],
            ClassType.fromId(result["class_type"]),
            result["source_link"]
        )

        suspend inline fun getSubclasses() = getSubclasses(className)
        suspend inline fun getSuperclasses() = getSuperclasses(className)
        suspend inline fun hasClassDoc() = index.docIndex.hasClassDoc(className)
    }

    inner class Method(val clazz: Class, val methodType: MethodType, val signature: String, val sourceLink: String) {
        val className: String
            get() = clazz.className
        val index: ImplementationIndex
            get() = this@ImplementationIndex

        constructor(result: DBResult) : this(
            Class(result),
            MethodType.fromId(result["method_type"]),
            result["signature"],
            result["method_source_link"]
        )

        suspend inline fun getImplementations() = getImplementations(clazz.className, signature)
        suspend inline fun getOverriddenMethods() = getOverriddenMethods(clazz.className, signature)
        suspend inline fun hasMethodDoc() = index.docIndex.hasMethodDoc(clazz.className, signature)
    }

    suspend fun getClass(className: FullSimpleClassName): Class? {
        return database.preparedStatement(
            """
                select c.class_name, c.class_type, c.source_link
                from class c
                where c.source_id = ?
                  and c.class_name = ?
            """.trimIndent(), readOnly = true
        ) {
            executeQuery(sourceType.id, className).readOrNull()?.let { Class(it) }
        }
    }

    suspend fun getMethod(className: String, signature: String): ImplementationIndex.Method? {
        return database.preparedStatement(
            """
                select c.class_name,
                       c.class_type,
                       c.source_link,
                       m.method_type,
                       m.signature,
                       m.source_link as method_source_link
                from class c
                         join method m on m.class_id = c.id
                where c.source_id = ?
                  and c.class_name = ?
                  and m.signature = ?
            """.trimIndent(), readOnly = true
        ) {
            executeQuery(sourceType.id, className, signature).readOrNull()?.let { Method(it) }
        }
    }

    suspend fun getSubclasses(className: FullSimpleClassName): List<Class> {
        return database.preparedStatement(
            """
                select subclass.class_name, subclass.class_type, subclass.source_link
                from class c
                         join subclass sub on sub.superclass_id = c.id
                         join class subclass on subclass.id = sub.subclass_id
                where c.source_id = ?
                  and c.class_name = ?
            """.trimIndent(), readOnly = true
        ) {
            executeQuery(sourceType.id, className).map { Class(it) }
        }
    }

    suspend fun getSuperclasses(className: FullSimpleClassName): List<Class> {
        return database.preparedStatement(
            """
                select superclass.class_name, superclass.class_type, superclass.source_link
                from class c
                         join subclass sub on sub.subclass_id = c.id
                         join class superclass on superclass.id = sub.superclass_id
                where c.source_id = ?
                  and c.class_name = ?
            """.trimIndent(), readOnly = true
        ) {
            executeQuery(sourceType.id, className).map { Class(it) }
        }
    }

    suspend fun getApiSubclasses(className: FullSimpleClassName): List<Class> {
        return database.preparedStatement(
            """
                with subclass as (select subclass.*
                                  from class superclass
                                           join subclass class_hierarchy on class_hierarchy.superclass_id = superclass.id
                                           join class subclass on class_hierarchy.subclass_id = subclass.id
                                  where superclass.source_id = ?
                                    and superclass.class_name = ?)
                select subclass.class_name, subclass.class_type, subclass.source_link
                from subclass
                         join doc on doc.classname = subclass.class_name -- Join on documented subclasses
                group by subclass.class_name, subclass.class_type, subclass.source_link
                order by length(subclass.class_name), subclass.class_name;
            """.trimIndent(), readOnly = true
        ) {
            executeQuery(sourceType.id, className).map { Class(it) }
        }
    }

    suspend fun getImplementations(className: FullSimpleClassName, methodSignature: String): List<Method> {
        return database.preparedStatement(
            """
                select implementation_owner.class_name,
                       implementation_owner.class_type,
                       implementation_owner.source_link,
                       implementation.method_type,
                       implementation.signature,
                       implementation.source_link as method_source_link
                from implementation impl
                         join class superclass on impl.class_id = superclass.id
                         join method implementation
                              on impl.implementation_id = implementation.id
                         join class implementation_owner
                              on implementation.class_id = implementation_owner.id
                where superclass.source_id = ?
                  and superclass.class_name = ?
                  and implementation.signature = ?
            """.trimIndent(), readOnly = true
        ) {
            executeQuery(sourceType.id, className, methodSignature).map { Method(it) }
        }
    }

    suspend fun getOverriddenMethods(className: FullSimpleClassName, methodSignature: String): List<Method> {
        return database.preparedStatement(
            """
                select c.class_name,
                       c.class_type,
                       c.source_link,
                       m.method_type,
                       m.signature,
                       m.source_link as method_source_link
                from implementation i
                         join method impl on i.implementation_id = impl.id
                         join class impl_owner on impl.class_id = impl_owner.id
                         join method m on i.method_id = m.id
                         join class c on m.class_id = c.id
                where impl_owner.source_id = ?
                  and impl_owner.class_name = ?
                  and impl.signature = ?
            """.trimIndent(), readOnly = true
        ) {
            executeQuery(sourceType.id, className, methodSignature).map { Method(it) }
        }
    }
}

val ImplementationIndex.Method.simpleQualifiedSignature: String
    get() = "$className#$signature"