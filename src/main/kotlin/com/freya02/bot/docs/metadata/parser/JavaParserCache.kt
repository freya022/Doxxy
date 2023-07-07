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
    private class Cache<T, K, V>(
        private val expectedItems: Int,
        container: MutableMap<K, V> = IdentityHashMap(calcMaxSize(expectedItems)),
        private val keySupplier: (T) -> K,
        private val valueSupplier: (T) -> V
    ) {
        private val map: MutableMap<K, V> = Collections.synchronizedMap(container)
        var hits = 0
            private set
        val expectedSize = calcMaxSize(expectedItems)
        val size: Int
            get() = map.size

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

    private fun <K, V> Cache(expectedMaxSize: Int, container: MutableMap<K, V> = IdentityHashMap(), valueSupplier: (K) -> V) =
        Cache(expectedMaxSize, container, { it }, valueSupplier)

    private fun <K, V> Cache(expectedMaxSize: Int, valueSupplier: (K) -> V)
        = Cache(expectedMaxSize, keySupplier = { it }, valueSupplier = valueSupplier)

    private fun <K, V> Cache(expectedMaxSize: Int, comparator: Comparator<K>, valueSupplier: (K) -> V)
        = Cache(expectedMaxSize, TreeMap(comparator), { it }, valueSupplier)

    //JP keeps recreating these map's keys,
    // but these keys are still being used multiple times to a point where it is still beneficial
    // An IdentityHashMap is used as JP *might* not implement hashCode/equals correctly, while using a property as a key would be counterproductive
    private val typeDeclarationQualifiedNames = Cache<ResolvedTypeDeclaration, String>(10232) { it.qualifiedName }
    private val referenceTypeQualifiedNames = Cache<ResolvedReferenceType, String>(5736) { it.qualifiedName }
    private val methodDeclarationSignatures = Cache<ResolvedMethodDeclaration, String>(4712) {
        it.erasedSimpleSignature
    }

    //A Comparator is used as the map keys are being recreated everytime, an IdentityHashMap would not work
    private val referenceTypeLightDeclaredMethods =
        Cache<ResolvedReferenceType, Set<ResolvedMethodDeclaration>>(345, Comparator.comparing { getQualifiedName(it) }) { it.lightDeclaredMethods }
    private val methodDeclarationDeclaringTypes =
        Cache<ResolvedMethodDeclaration, ResolvedReferenceTypeDeclaration>(9690) { it.declaringType() }

    private val parameterDeclarationTypes =
        Cache<ResolvedParameterDeclaration, Parameter, ResolvedType>(
            expectedItems = 2057,
            keySupplier = { it.toAst(Parameter::class.java).get() },
            valueSupplier = { it.type }
        )

    private val referenceTypeDirectAncestors =
        Cache<ResolvedReferenceType, List<ResolvedReferenceType>>(365, Comparator.comparing { getQualifiedName(it) }) { it.directAncestors }

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
        logger.trace("Cache info:")
        this::class.declaredMemberProperties
            .filter { it.returnType.jvmErasure == Cache::class }
            .forEach {
                val cache: Cache<*, *, *> = it.javaField?.get(this) as? Cache<*, *, *>
                    ?: return logger.warn("Could not get field '${it.name}'")
                logger.trace { "${it.name}: $cache" }

                //Cache's map was expanded
                if (cache.expectedSize < cache.size) {
                    logger.warn { "Cache ${it.name} was configured to cache ${cache.expectedSize} items but caches ${cache.size} items" }
                }

                //Cache is not used
                if (cache.hits < cache.size) {
                    logger.warn { "Cache ${it.name} has only ${cache.hits} but caches ${cache.size} items" }
                }
            }
    }



    private companion object {
        private val logger = KotlinLogging.logger { }

        //Used to avoid resizing IdentityHashMap, the sizes comes from the map's sizes when parsing JDA 5 beta 5
        // Takes the nearest power of 2 toward positive infinite
        private fun calcMaxSize(size: Int) = 1 shl ceil(log2(size.toDouble())).toInt()
    }
}