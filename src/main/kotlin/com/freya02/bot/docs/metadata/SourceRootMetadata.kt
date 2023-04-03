package com.freya02.bot.docs.metadata

import com.freya02.bot.docs.metadata.data.ClassMetadata
import com.freya02.bot.docs.metadata.data.FieldMetadata
import com.freya02.bot.docs.metadata.data.MethodMetadata
import com.freya02.bot.utils.createProfiler
import com.freya02.bot.utils.nestedProfiler
import com.freya02.bot.utils.nextStep
import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.symbolsolver.JavaSymbolSolver
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver
import com.github.javaparser.utils.SourceRoot
import mu.KotlinLogging
import java.nio.file.Path

class SourceRootMetadata(sourceRootPath: Path) {
    private val sourceRoot: SourceRoot = SourceRoot(sourceRootPath)

    private val solver = JavaSymbolSolver(CombinedTypeSolver().also { combinedTypeSolver ->
        //This solver isn't needed to resolve implementations, but is used to get back the AST nodes (and thus, the ranges)
        combinedTypeSolver.add(JavaParserTypeSolver(sourceRootPath))
        combinedTypeSolver.add(ReflectionTypeSolver(/* jreOnly = */ false))
    })

    private val classMetadataMap: Map<ClassName, ClassMetadata>
    val implementationMetadata: ImplementationMetadata

    init {
        createProfiler("SourceRootMetadata") {
            nextStep("Make solver") {
                sourceRoot.parserConfiguration.setSymbolResolver(solver)
            }

            val compilationUnits: List<CompilationUnit> = nextStep("Parse") {
                sourceRoot
                    .tryToParseParallelized("net.dv8tion.jda")
                    .filter { result ->
                        if (result.problems.isNotEmpty()) {
                            result.problems.forEach { Companion.logger.error(it.toString()) }
                        } else if (!result.isSuccessful) {
                            Companion.logger.error("Unexpected failure while parsing CU")
                        }

                        result.isSuccessful
                    }.map { it.result.get() }
            }

            nestedProfiler("Class metadata") {
                classMetadataMap = ClassMetadataParser.parse(sourceRoot)
            }

            nextStep("Implementation metadata") {
                implementationMetadata = ImplementationMetadataParser.parseCompilationUnits(compilationUnits)
            }
        }
    }

    fun getMethodsParameters(className: ClassName, methodName: String): List<MethodMetadata> {
        return classMetadataMap[className]
            ?.methodMetadataMap
            ?.get(methodName)
            ?: emptyList()
    }

    fun getFieldMetadata(className: ClassName, fieldName: String): FieldMetadata? {
        return classMetadataMap[className]
            ?.fieldMetadataMap
            ?.get(fieldName)
    }

    companion object {
        private val logger = KotlinLogging.logger { }
    }
}