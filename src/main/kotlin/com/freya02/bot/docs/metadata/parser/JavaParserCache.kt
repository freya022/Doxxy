package com.freya02.bot.docs.metadata.parser

import com.github.javaparser.ast.body.Parameter
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration
import com.github.javaparser.resolution.declarations.ResolvedParameterDeclaration
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration
import com.github.javaparser.resolution.declarations.ResolvedTypeDeclaration
import com.github.javaparser.resolution.types.ResolvedReferenceType
import com.github.javaparser.resolution.types.ResolvedType
import mu.KotlinLogging
import java.util.*
import kotlin.math.ceil
import kotlin.math.log2
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.jvm.javaField
import kotlin.reflect.jvm.jvmErasure

class JavaParserCache {
    private class Cache<T, K, V>(container: MutableMap<K, V> = IdentityHashMap(), private val keySupplier: (T) -> K, private val valueSupplier: (T) -> V) {
        private val map: MutableMap<K, V> = Collections.synchronizedMap(container)
        private var hits = 0

        operator fun get(t: T): V {
            hits++
            return map.getOrPut(keySupplier(t)) {
                hits--
                valueSupplier(t)
            }
        }

        override fun toString(): String {
            return "Size=${map.size}, hits=$hits"
        }
    }

    private fun <K, V> Cache(container: MutableMap<K, V> = IdentityHashMap(), valueSupplier: (K) -> V) =
        Cache(container, { it }, valueSupplier)

    private fun <K, V> Cache(expectedMaxSize: Int, valueSupplier: (K) -> V)
        = Cache(IdentityHashMap(expectedMaxSize), { it }, valueSupplier)

    private fun <K, V> Cache(comparator: Comparator<K>, valueSupplier: (K) -> V)
        = Cache(TreeMap(comparator), { it }, valueSupplier)

    //JP keeps recreating these map's keys,
    // but these keys are still being used multiple times to a point where it is still beneficial
    // An IdentityHashMap is used as JP *might* not implement hashCode/equals correctly, while using a property as a key would be counterproductive
    private val typeDeclarationQualifiedNames = Cache<ResolvedTypeDeclaration, String>(calcMaxSize(10232.0)) { it.qualifiedName }
    private val referenceTypeQualifiedNames = Cache<ResolvedReferenceType, String>(calcMaxSize(3869.0)) { it.qualifiedName }
    private val methodDeclarationSignatures = Cache<ResolvedMethodDeclaration, String>(calcMaxSize(4712.0)) {
        it.buildSignature()
    }

    //A Comparator is used as the map keys are being recreated everytime, an IdentityHashMap would not work
    private val referenceTypeLightDeclaredMethods =
        Cache<ResolvedReferenceType, Set<ResolvedMethodDeclaration>>(Comparator.comparing { getQualifiedName(it) }) { it.lightDeclaredMethods }
    private val methodDeclarationDeclaringTypes =
        Cache<ResolvedMethodDeclaration, ResolvedReferenceTypeDeclaration>(calcMaxSize(9690.0)) { it.declaringType() }

    private val parameterDeclarationTypes =
        Cache<ResolvedParameterDeclaration, Parameter, ResolvedType>(
            container = IdentityHashMap(calcMaxSize(2057.0)),
            keySupplier = { it.toAst(Parameter::class.java).get() },
            valueSupplier = { it.type }
        )

    private val referenceTypeDirectAncestors =
        Cache<ResolvedReferenceType, List<ResolvedReferenceType>>(Comparator.comparing { getQualifiedName(it) }) { it.directAncestors }

    fun getQualifiedName(declaration: ResolvedTypeDeclaration): String = typeDeclarationQualifiedNames[declaration]

    fun getQualifiedName(declaration: ResolvedReferenceType): String = referenceTypeQualifiedNames[declaration]

    fun getSignature(declaration: ResolvedMethodDeclaration): String =
        methodDeclarationSignatures[declaration]

    fun getLightDeclaredMethods(declaration: ResolvedReferenceType): Set<ResolvedMethodDeclaration> =
        referenceTypeLightDeclaredMethods[declaration]

    fun getDeclaringType(usage: ResolvedMethodDeclaration): ResolvedReferenceTypeDeclaration = methodDeclarationDeclaringTypes[usage]

    fun getType(declaration: ResolvedParameterDeclaration): ResolvedType = parameterDeclarationTypes[declaration]

    fun getDirectAncestors(declaration: ResolvedReferenceType): List<ResolvedReferenceType>
            = referenceTypeDirectAncestors[declaration]

    fun logCaches() {
        logger.info("Cache info:")
        this::class.declaredMemberProperties
            .filter { it.returnType.jvmErasure == Cache::class }
            .forEach {
                val cache: Cache<*, *, *> = it.javaField?.get(this) as? Cache<*, *, *>
                    ?: return logger.warn("Could not get field '${it.name}'")
                logger.info("${it.name}: $cache")
            }
    }

    private fun ResolvedMethodDeclaration.buildSignature(): String = buildString {
        append(name)
        append('(')
        for (i in 0..<numberOfParams) {
            append(getParam(i).type.describe())
        }
        append(')')
    }

    //Used to avoid resizing IdentityHashMap, the sizes comes from the map's sizes when parsing JDA 5 beta 5
    // Takes the nearest power of 2 toward positive infinite
    private fun calcMaxSize(size: Double) = 1 shl ceil(log2(size)).toInt()

    private companion object {
        private val logger = KotlinLogging.logger { }
    }
}