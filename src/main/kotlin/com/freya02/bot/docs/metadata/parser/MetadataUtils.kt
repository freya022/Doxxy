package com.freya02.bot.docs.metadata.parser

import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.nodeTypes.NodeWithName
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration
import com.github.javaparser.resolution.types.ResolvedPrimitiveType
import com.github.javaparser.resolution.types.ResolvedReferenceType
import mu.KLogger
import org.intellij.lang.annotations.Language
import org.postgresql.copy.CopyManager
import org.postgresql.core.BaseConnection
import java.sql.Connection
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

//This is a light version of ResolvedReferenceType#getDeclaredMethods
// Avoid allocating MethodUsage instances as we don't need anything from them
// All the getters used were simple delegates
val ResolvedReferenceType.lightDeclaredMethods: Set<ResolvedMethodDeclaration>
    get() = typeDeclaration.getOrNull()?.declaredMethods ?: emptySet()

fun List<CompilationUnit>.forEachCompilationUnit(logger: KLogger, block: (CompilationUnit) -> Unit) {
    forEach { unit ->
        kotlin.runCatching { block(unit) }.onFailure {
            logger.error("Failed to parse CU ${unit.debugFQCN}", it)
        }
    }
}

fun isMethodCompatible(
    cache: JavaParserCache,
    subMethod: ResolvedMethodDeclaration,
    superMethod: ResolvedMethodDeclaration
): Boolean {
    if (subMethod.name != superMethod.name) return false
    if (subMethod.numberOfParams != superMethod.numberOfParams) return false

    return (0 until superMethod.numberOfParams).all { i ->
        val subType = cache.getType(subMethod.getParam(i))
        when (val superType = cache.getType(superMethod.getParam(i))) {
            //JP says that converting an int to a long is possible, but what we want is to check the types
            // Check primitive name if it is one instead
            is ResolvedPrimitiveType -> subType.isPrimitive
                    && superType.describe() == subType.asPrimitive().describe()

            else -> superType.isAssignableBy(subType)
        }
    }
}

//See ResolvedReferenceTypeDeclaration#breadthFirstFunc
fun ResolvedReferenceTypeDeclaration.getAllAncestorsOptimized(cache: JavaParserCache): List<ResolvedReferenceType> {
    val ancestors: MutableList<ResolvedReferenceType> = arrayListOf()
    // We want to avoid infinite recursion in case of Object having Object as ancestor
    if (!this.isJavaLangObject) {
        // init direct ancestors
        val queuedAncestors: Deque<ResolvedReferenceType> = LinkedList(this.ancestors) //Cache is not needed, this is called at most once per class
        ancestors.addAll(queuedAncestors)
        while (!queuedAncestors.isEmpty()) {
            val queuedAncestor = queuedAncestors.removeFirst()
            if (queuedAncestor.typeDeclaration.isPresent) {
                LinkedHashSet(cache.getDirectAncestors(queuedAncestor)).forEach { ancestor: ResolvedReferenceType ->
                    // add this ancestor to the queue (for a deferred search)
                    queuedAncestors.add(ancestor)
                    // add this ancestor to the list of ancestors
                    if (!ancestors.contains(ancestor)) {
                        ancestors.add(ancestor)
                    }
                }
            }
        }
    }
    return ancestors
}

fun Connection.copyFrom(@Language("PostgreSQL", prefix = "copy ", suffix = " from stdin delimiter ','") table: String, list: Collection<Collection<Any?>>) {
    CopyManager(unwrap(BaseConnection::class.java))
        .copyIn("copy $table from stdin delimiter ','", list.joinToString("\n") { it.joinToString(",") }.byteInputStream())
}