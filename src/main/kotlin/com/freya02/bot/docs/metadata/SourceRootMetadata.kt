package com.freya02.bot.docs.metadata

import com.freya02.bot.docs.metadata.data.ClassMetadata
import com.freya02.bot.docs.metadata.data.FieldMetadata
import com.freya02.bot.docs.metadata.data.MethodMetadata
import com.freya02.bot.utils.createProfiler
import com.freya02.bot.utils.nestedProfiler
import com.freya02.bot.utils.nextStep
import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.utils.SourceRoot
import mu.KotlinLogging
import java.nio.file.Path

class SourceRootMetadata(sourceRootPath: Path, binariesPath: List<Path>) {
    private val sourceRoot: SourceRoot = SourceRoot(sourceRootPath)

    private val classMetadataMap: Map<ClassName, ClassMetadata>
    val implementationMetadata: ImplementationMetadata

    init {
        createProfiler("SourceRootMetadata") {
            val compilationUnits: List<CompilationUnit> = nextStep("Parse") {
                sourceRoot
                    .tryToParse("net.dv8tion.jda")
                    .filter { result ->
                        if (result.problems.isNotEmpty()) {
                            result.problems.forEach { Companion.logger.error(it.toString()) }
                        } else if (!result.isSuccessful) {
                            Companion.logger.error("Unexpected failure while parsing CU")
                        }

                        result.isSuccessful
                    }.map { it.result.get() }
            }

            nestedProfiler("ClassMetadataParser") {
                classMetadataMap = ClassMetadataParser.parse(sourceRoot)
            }

            nextStep("Implementation metadata") {
                implementationMetadata = ImplementationMetadata.fromClasspath(binariesPath)
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