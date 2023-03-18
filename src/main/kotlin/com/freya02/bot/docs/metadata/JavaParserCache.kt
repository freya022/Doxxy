package com.freya02.bot.docs.metadata

import com.github.javaparser.resolution.MethodUsage
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration
import com.github.javaparser.resolution.declarations.ResolvedTypeDeclaration
import com.github.javaparser.resolution.types.ResolvedReferenceType
import java.util.*

class JavaParserCache {
    private class Cache<K, V>(container: MutableMap<K, V> = IdentityHashMap(), private val valueSupplier: (K) -> V) {
        private val map: MutableMap<K, V> = Collections.synchronizedMap(container)
        private var hits = 0

        operator fun get(k: K): V {
            hits++
            return map.getOrPut(k) {
                hits--
                valueSupplier(k)
            }
        }

        override fun toString(): String {
            return "Size=${map.size}, hits=$hits"
        }
    }

    //JP keeps recreating these map's keys,
    // but these keys are still being used multiple times to a point where it is still beneficial
    // An IdentityHashMap is used as JP *might* not implement hashCode/equals correctly, while using a property as a key would be counterproductive
    private val typeDeclarationQualifiedNames = Cache<ResolvedTypeDeclaration, String> { it.qualifiedName }
    private val referenceTypeQualifiedNames = Cache<ResolvedReferenceType, String> { it.qualifiedName }
    private val methodDeclarationQualifiedDescriptors =
        Cache<ResolvedMethodDeclaration, String> { it.qualifiedName + it.fixedDescriptor }

    private val referenceTypeDeclaredMethods = Cache<ResolvedReferenceType, Set<MethodUsage>>() { it.declaredMethods }

    fun getQualifiedName(declaration: ResolvedTypeDeclaration): String = typeDeclarationQualifiedNames[declaration]

    fun getQualifiedName(declaration: ResolvedReferenceType): String = referenceTypeQualifiedNames[declaration]

    fun getQualifiedDescriptor(declaration: ResolvedMethodDeclaration): String =
        methodDeclarationQualifiedDescriptors[declaration]

    fun getDeclaredMethods(declaration: ResolvedReferenceType): Set<MethodUsage> =
        referenceTypeDeclaredMethods[declaration]

    private val ResolvedMethodDeclaration.fixedDescriptor: String
        get() = buildString {
            append('(')
            for (i in 0..<numberOfParams) {
                append(getParam(i).type.describe())
            }
            append(')')

            append(returnType.describe())
        }
}