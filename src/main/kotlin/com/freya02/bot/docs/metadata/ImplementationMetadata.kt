package com.freya02.bot.docs.metadata

import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration

class ImplementationMetadata(val classes: Map<String, Class>) {
    fun getClassByQualifiedName(qualifiedName: String) = classes[qualifiedName]
    fun getClassBySimpleName(simpleName: String) = classes.entries.first { it.key.endsWith(".$simpleName") }.value

    class Class(val declaration: ResolvedReferenceTypeDeclaration, val qualifiedName: String) {
        val subclasses: MutableSet<Class> = hashSetOf()
        val superclasses: MutableSet<Class> = hashSetOf()
        val methods: MutableMap<String, Method> = hashMapOf()

        fun getSubclassByQualifiedName(qualifiedName: String) = subclasses.first { it.qualifiedName == qualifiedName }
        fun getSubclassBySimpleName(simpleName: String) = subclasses.first { it.qualifiedName.endsWith(".$simpleName") }

        fun getMethodsByName(name: String) = methods.filterKeys { it.startsWith(name) }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Class

            if (qualifiedName != other.qualifiedName) return false

            return true
        }

        override fun hashCode(): Int {
            return qualifiedName.hashCode()
        }

        override fun toString(): String = qualifiedName
    }

    class Method(val declaration: ResolvedMethodDeclaration, val owner: Class, val descriptor: String) {
        val name: String = declaration.name
        val qualifiedDescriptor = owner.qualifiedName + "." + descriptor
        val implementations: MutableSet<Method> = hashSetOf()

        val range: IntRange
            get() {
                val node = declaration.toAst().get()
                return node.begin.get().line..node.end.get().line
            }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Method

            if (owner != other.owner) return false
            if (qualifiedDescriptor != other.qualifiedDescriptor) return false

            return true
        }

        override fun hashCode(): Int {
            var result = owner.hashCode()
            result = 31 * result + qualifiedDescriptor.hashCode()
            return result
        }

        override fun toString(): String = owner.qualifiedName + "." + qualifiedDescriptor
    }
}