package com.freya02.bot.docs.metadata

import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration
import com.github.javaparser.resolution.declarations.ResolvedTypeDeclaration
import com.github.javaparser.resolution.types.ResolvedReferenceType
import java.util.*
import kotlin.math.ceil
import kotlin.math.log2

class JavaParserCache {
    private class Cache<K, V>(container: MutableMap<K, V> = IdentityHashMap(), private val valueSupplier: (K) -> V) {
        constructor(expectedMaxSize: Int, valueSupplier: (K) -> V) : this(IdentityHashMap(expectedMaxSize), valueSupplier)
        constructor(comparator: Comparator<K>, valueSupplier: (K) -> V) : this(TreeMap(comparator), valueSupplier)

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
    private val typeDeclarationQualifiedNames = Cache<ResolvedTypeDeclaration, String>(calcMaxSize(10121.0)) { it.qualifiedName }
    private val referenceTypeQualifiedNames = Cache<ResolvedReferenceType, String>(calcMaxSize(8477.0)) { it.qualifiedName }
    private val methodDeclarationQualifiedDescriptors = Cache<ResolvedMethodDeclaration, String>(calcMaxSize(2049.0)) {
        it.buildQualifiedDescriptor()
    }

    //A Comparator is used as the map keys are being recreated everytime, an IdentityHashMap would not work
    private val referenceTypeLightDeclaredMethods =
        Cache<ResolvedReferenceType, Set<ResolvedMethodDeclaration>>(Comparator.comparing { getQualifiedName(it) }) { it.lightDeclaredMethods }
    private val methodUsageDeclaringTypes =
        Cache<ResolvedMethodDeclaration, ResolvedReferenceTypeDeclaration>(calcMaxSize(10028.0)) { it.declaringType() }

    fun getQualifiedName(declaration: ResolvedTypeDeclaration): String = typeDeclarationQualifiedNames[declaration]

    fun getQualifiedName(declaration: ResolvedReferenceType): String = referenceTypeQualifiedNames[declaration]

    fun getQualifiedDescriptor(declaration: ResolvedMethodDeclaration): String =
        methodDeclarationQualifiedDescriptors[declaration]

    fun getLightDeclaredMethods(declaration: ResolvedReferenceType): Set<ResolvedMethodDeclaration> =
        referenceTypeLightDeclaredMethods[declaration]

    fun getDeclaringType(usage: ResolvedMethodDeclaration): ResolvedReferenceTypeDeclaration = methodUsageDeclaringTypes[usage]

    private fun ResolvedMethodDeclaration.buildQualifiedDescriptor(): String = buildString {
        append(qualifiedName)
        append('(')
        for (i in 0..<numberOfParams) {
            append(getParam(i).type.describe())
        }
        append(')')

        append(returnType.describe())
    }

    //Used to avoid resizing IdentityHashMap, the sizes comes from the map's sizes when parsing JDA 5 beta 5
    // Takes the nearest power of 2 toward positive infinite
    private fun calcMaxSize(size: Double) = 1 shl ceil(log2(size)).toInt()
}