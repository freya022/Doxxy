package com.freya02.bot.docs.metadata

import com.freya02.bot.docs.index.DocIndex
import com.freya02.bot.docs.metadata.parser.FullSimpleClassName
import com.freya02.botcommands.api.core.db.DBResult
import com.freya02.botcommands.api.core.db.Database
import com.freya02.docs.DocSourceType

class ImplementationIndex(val docIndex: DocIndex, private val database: Database) {
    val sourceType: DocSourceType
        get() = docIndex.sourceType

    inner class Class(val className: String, val classType: ClassType, val sourceLink: String) {
        val index: ImplementationIndex
            get() = this@ImplementationIndex

        constructor(result: DBResult) : this(result["class_name"], ClassType.fromId(result["class_type"]), result["source_link"])

        suspend inline fun getSubclasses() = getSubclasses(className)
        suspend inline fun getSuperclasses() = getSuperclasses(className)
        suspend inline fun hasClassDoc() = index.docIndex.hasClassDoc(className)
    }

    //TODO replace className with Class object
    inner class Method(val className: String, val signature: String, val sourceLink: String) {
        val index: ImplementationIndex
            get() = this@ImplementationIndex

        constructor(result: DBResult) : this(result["class_name"], result["signature"], result["source_link"])

        suspend inline fun getImplementations() = getImplementations(className, signature)
        suspend inline fun hasMethodDoc() = index.docIndex.getMethodDoc(className, signature) != null //TODO optimize
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
            executeQuery(sourceType.id, className).readOnce()?.let { Class(it) }
        }
    }

    suspend fun getMethod(className: String, signature: String): ImplementationIndex.Method? {
        return database.preparedStatement(
            """
                select c.class_name, m.signature, m.source_link
                from class c
                         join method m on m.class_id = c.id
                where c.source_id = ?
                  and c.class_name = ?
                  and m.signature = ?
            """.trimIndent(), readOnly = true
        ) {
            executeQuery(sourceType.id, className, signature).readOnce()?.let { Method(it) }
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

    suspend fun getImplementations(className: FullSimpleClassName, methodSignature: String): List<Method> {
        return database.preparedStatement(
            """
                select implementation_owner.class_name,
                       implementation.signature,
                       implementation.source_link
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
}