package dev.freya02.doxxy.bot.docs.metadata.parser

import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.symbolsolver.JavaSymbolSolver
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver
import com.github.javaparser.utils.SourceRoot
import dev.freya02.doxxy.bot.docs.metadata.data.ClassMetadata
import dev.freya02.doxxy.bot.docs.metadata.data.FieldMetadata
import dev.freya02.doxxy.bot.docs.metadata.data.MethodMetadata
import dev.freya02.doxxy.bot.utils.createProfiler
import dev.freya02.doxxy.bot.utils.nestedProfiler
import dev.freya02.doxxy.bot.utils.nextStep
import java.nio.file.Path

class SourceMetadata(sourceRootPath: Path) {
    private val classMetadataMap: Map<ClassName, ClassMetadata>
    val implementationMetadata: ImplementationMetadata

    init {
        createProfiler("SymbolSolverSourceMetadata") {
            val sourceRoot = SourceRoot(sourceRootPath)

            nextStep("Make solver") {
                val solver = JavaSymbolSolver(CombinedTypeSolver().also { combinedTypeSolver ->
                    //This solver isn't needed to resolve implementations, but is used to get back the AST nodes (and thus, the ranges)
                    combinedTypeSolver.add(JavaParserTypeSolver(sourceRootPath))
                    combinedTypeSolver.add(ReflectionTypeSolver(/* jreOnly = */ false))
                })

                sourceRoot.parserConfiguration.setSymbolResolver(solver)
            }

            val compilationUnits: List<CompilationUnit> = nextStep("Parse") {
                sourceRoot
                    .tryToParseParallelized("net.dv8tion.jda")
                    .filter { result ->
                        if (result.problems.isNotEmpty()) {
                            result.problems.forEach { logger.error(it.toString()) }
                        } else if (!result.isSuccessful) {
                            logger.error("Unexpected failure while parsing CU")
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
}