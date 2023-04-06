package com.freya02.bot.docs.metadata.parser

import com.freya02.bot.docs.metadata.ClassType
import com.freya02.bot.docs.metadata.MethodType
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration

class ImplementationMetadata(val classes: Map<String, Class>) {
    fun getClassByQualifiedName(qualifiedName: String) = classes[qualifiedName]
    fun getClassBySimpleName(simpleName: String) = classes.entries.first { it.key.endsWith(".$simpleName") }.value

    class Class(val declaration: ResolvedReferenceTypeDeclaration, val qualifiedName: String) {
        val classType = ClassType.fromDeclaration(declaration)
        val packageName: PackageName
        val name: FullSimpleClassName
        val topLevelName: TopSimpleClassName

        val subclasses: MutableSet<Class> = hashSetOf()
        val superclasses: MutableSet<Class> = hashSetOf()
        val declaredMethods: MutableMap<Signature, Method> = hashMapOf()

        val methods: Set<Method> by lazy {
            (declaredMethods.values + superclasses.flatMap { it.methods }).toSet()
        }

        init {
            val segments = qualifiedName.split('.')
            val classIndex = segments.indexOfLast { it.all { c -> c.isLowerCase() || !c.isLetter() } } + 1
            packageName = segments.take(classIndex).joinToString(".")
            topLevelName = segments[classIndex] //Message in Message.Attachment
            name = segments.drop(classIndex).joinToString(".")
        }

        fun getSubclassByQualifiedName(qualifiedName: String) = subclasses.first { it.qualifiedName == qualifiedName }
        fun getSubclassBySimpleName(simpleName: String) = subclasses.first { it.qualifiedName.endsWith(".$simpleName") }

        fun getDeclaredMethodsByName(name: String) = declaredMethods.filterKeys { it.substringBefore('(') == name }
        fun getMethodsByName(name: String) = methods.filter { it.name == name }

        fun isSubclassOf(clazz: Class): Boolean = clazz == this || clazz in superclasses

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

    class Method(declaration: ResolvedMethodDeclaration, val owner: Class, val signature: String) {
        val type: MethodType = MethodType.fromDeclaration(declaration)
        val name: String = declaration.name
        val qualifiedSignature = owner.qualifiedName + "." + signature
        val implementations: MutableSet<Method> = hashSetOf()

        val range: IntRange = declaration.toAst().get().let { node ->
            node.begin.get().line..node.end.get().line
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Method

            if (owner != other.owner) return false
            if (signature != other.signature) return false

            return true
        }

        override fun hashCode(): Int {
            var result = owner.hashCode()
            result = 31 * result + signature.hashCode()
            return result
        }

        override fun toString(): String = qualifiedSignature
    }
}