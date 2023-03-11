package com.freya02.bot.docs.metadata

import com.github.javaparser.ParseResult
import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.nodeTypes.NodeWithName
import mu.KLogger

val NodeWithName<*>.fullPackageName: String
    //Split name parts and take while the first char is a lower case (i.e. a package)
    get() = nameAsString.split(".").takeWhile { it[0].isLowerCase() }.joinToString(".")

val NodeWithName<*>.fullClassName: String
    //Split name parts and take from the end while the first char is an upper case (i.e. a class name)
    get() = nameAsString.split(".").takeLastWhile { it[0].isUpperCase() }.joinToString(".")

fun List<ParseResult<CompilationUnit>>.forEachCompilationUnit(logger: KLogger, block: (CompilationUnit) -> Unit) {
    forEach { result ->
        if (result.problems.isNotEmpty()) {
            result.problems.forEach { logger.error(it.toString()) }
        }
        result.ifSuccessful { unit ->
            kotlin.runCatching { block(unit) }.onFailure {
                logger.error("Failed to parse CU $result", it)
            }
        }
    }
}