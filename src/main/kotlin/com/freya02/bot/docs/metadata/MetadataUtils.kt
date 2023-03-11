package com.freya02.bot.docs.metadata

import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.nodeTypes.NodeWithName
import mu.KLogger
import kotlin.jvm.optionals.getOrNull

val CompilationUnit.debugFQCN: String
    get() = "${packageDeclaration.getOrNull()}.${primaryTypeName.getOrNull()}"

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