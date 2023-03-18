package com.freya02.bot.docs.metadata

import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.nodeTypes.NodeWithName
import com.github.javaparser.resolution.MethodUsage
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration
import com.github.javaparser.resolution.types.ResolvedPrimitiveType
import com.github.javaparser.resolution.types.ResolvedReferenceType
import mu.KLogger
import java.util.*
import kotlin.jvm.optionals.getOrNull

val CompilationUnit.debugFQCN: String
    get() = "${packageDeclaration.getOrNull()?.nameAsString?.substringBefore(';')}.${primaryTypeName.getOrNull()}"

val NodeWithName<*>.fullPackageName: String
    //Split name parts and take while the first char is a lower case (i.e. a package)
    get() = nameAsString.split(".").takeWhile { it[0].isLowerCase() }.joinToString(".")

val NodeWithName<*>.fullClassName: String
    //Split name parts and take from the end while the first char is an upper case (i.e. a class name)
    get() = nameAsString.split(".").takeLastWhile { it[0].isUpperCase() }.joinToString(".")

fun List<CompilationUnit>.forEachCompilationUnit(logger: KLogger, block: (CompilationUnit) -> Unit) {
    forEach { unit ->
        kotlin.runCatching { block(unit) }.onFailure {
            logger.error("Failed to parse CU ${unit.debugFQCN}", it)
        }
    }
}

fun isMethodCompatible(subMethod: ResolvedMethodDeclaration, superMethod: MethodUsage): Boolean {
    if (subMethod.name != superMethod.name) return false
    if (subMethod.numberOfParams != superMethod.noParams) return false

    return (0 until superMethod.noParams).all { i ->
        when (val superType = superMethod.getParamType(i)) {
            //JP says that converting an int to a long is possible, but what we want is to check the types
            // Check primitive name if it is one instead
            is ResolvedPrimitiveType -> subMethod.getParam(i).type.isPrimitive
                    && superType.describe() == subMethod.getParam(i).type.asPrimitive().describe()

            else -> superType.isAssignableBy(subMethod.getParam(i).type)
        }
    }
}

fun <K, V> Comparator<K>.createMap(): MutableMap<K, V> = Collections.synchronizedMap(TreeMap(this))
fun <E> Comparator<E>.createSet(): MutableSet<E> = Collections.synchronizedSet(TreeSet(this))

fun <T> Map<ResolvedReferenceType, T>.findRefByClassName(name: String): T {
    return this.toList().first { (k, _) -> k.qualifiedName.endsWith(".$name") }.second
}

fun <T> Map<ResolvedReferenceTypeDeclaration, T>.findDeclByClassName(name: String): T {
    return this.toList().first { (k, _) -> k.qualifiedName.endsWith(".$name") }.second
}

fun <T> Map<ResolvedMethodDeclaration, T>.findByMethodName(name: String): Map<String, T> {
    return this.filterKeys { it.name == name }.mapKeys { (k, _) -> k.className }
}

fun <T> Map<T, Iterable<ResolvedReferenceTypeDeclaration>>.flattenReferences() =
    map { (k, v) -> v.map { it.qualifiedName } }.flatten()