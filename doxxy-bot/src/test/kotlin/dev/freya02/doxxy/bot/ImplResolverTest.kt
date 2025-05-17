package dev.freya02.doxxy.bot

import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration
import com.github.javaparser.resolution.types.ResolvedReferenceType
import com.github.javaparser.symbolsolver.JavaSymbolSolver
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver
import com.github.javaparser.utils.SourceRoot
import dev.freya02.doxxy.bot.docs.metadata.parser.ImplementationMetadataParser
import io.github.oshai.kotlinlogging.KotlinLogging
import java.nio.file.Path


private typealias ResolvedClass = ResolvedReferenceType
private typealias ResolvedMethod = ResolvedMethodDeclaration

object ImplResolverTest {
    private val logger = KotlinLogging.logger { }

//    private val rootPath = Data.javadocsPath.resolve("JDA")
    private val rootPath: Path = extractJDASources()

    private val sourceRoot = SourceRoot(rootPath)
    private val solver = JavaSymbolSolver(CombinedTypeSolver().also { combinedTypeSolver ->
        combinedTypeSolver.add(JavaParserTypeSolver(rootPath))
        combinedTypeSolver.add(ReflectionTypeSolver(/* jreOnly = */ false))
    })

    @JvmStatic
    fun main(args: Array<String>) {
        sourceRoot.parserConfiguration.setSymbolResolver(solver)
        sourceRoot
            .tryToParse("net.dv8tion.jda")
            .filter { result ->
                if (result.problems.isNotEmpty()) {
                    result.problems.forEach { logger.error { it.toString() } }
                } else if (!result.isSuccessful) {
                    logger.error { "Unexpected failure while parsing CU" }
                }

                result.isSuccessful
            }.map { it.result.get() }

        val implementationMetadata = ImplementationMetadataParser.parseCompilationUnits(sourceRoot.compilationUnits)

        println()
    }
}