package com.freya02.docs

import com.freya02.bot.versioning.VersionsUtils
import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration
import com.github.javaparser.resolution.types.ResolvedReferenceType
import com.github.javaparser.symbolsolver.JavaSymbolSolver
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver
import com.github.javaparser.utils.SourceRoot
import java.io.FileNotFoundException
import java.nio.file.Path
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.io.path.*
import kotlin.system.measureTimeMillis


private typealias ResolvedClass = ResolvedReferenceType
private typealias ResolvedMethod = ResolvedMethodDeclaration

object ImplResolverTest {
//    private val rootPath = Data.javadocsPath.resolve("JDA")
    private val rootPath: Path = run {
        System.getProperty("java.class.path")
            .split(System.getProperty("path.separator"))
            .map { Path(it) }
            .single { it.name.startsWith("JDA-5.0.0") }
            .let { it.resolveSibling(it.nameWithoutExtension + "-sources.jar") }
            .also { p ->
                if (p.notExists())
                    throw FileNotFoundException("Source JAR at ${p.absolutePathString()} does not exist.")
            }
//            .let { FileSystems.newFileSystem(it).getPath("") } //JavaParser doesn't support ZipFileSystem
            .let { zipPath ->
                val tmpSourcesPath = Path(System.getProperty("java.io.tmpdir"), "JDA_sources")
                if (tmpSourcesPath.exists())
                    return@let tmpSourcesPath

                tmpSourcesPath.createDirectories().also {
                    VersionsUtils.extractZip(zipPath, it, "java")
                }
            }
    }
    private val sourceRoot = SourceRoot(rootPath)
    private val solver = JavaSymbolSolver(CombinedTypeSolver().also { combinedTypeSolver ->
        combinedTypeSolver.add(JavaParserTypeSolver(rootPath))
        combinedTypeSolver.add(ReflectionTypeSolver(/* jreOnly = */ false))
    })

    private val subclassesMap: MutableMap<ResolvedClass, MutableList<ResolvedReferenceTypeDeclaration>> = ConcurrentHashMap()

    // BaseClass -> Map<TheMethod, List<ClassOverridingMethod>>
    private val classToMethodImplementations: MutableMap<ResolvedClass, MutableMap<ResolvedMethod, MutableList<ResolvedReferenceTypeDeclaration>>> = ConcurrentHashMap()

    fun <T> Map<ResolvedClass, T>.findByClassName(name: String): T {
        return this.toList().first { (k, _) -> k.qualifiedName.endsWith(".$name") }.second
    }

    fun <T> Map<ResolvedMethod, T>.findByMethodName(name: String): Map<String, T> {
        return this.filterKeys { it.name == name }.mapKeys { (k, _) -> k.className }
    }

    @JvmStatic
    fun main(args: Array<String>) {
        measureTimeMillis {
            sourceRoot.parserConfiguration.setSymbolResolver(solver)
            sourceRoot.parseParallelized("net.dv8tion.jda") { _, _, result ->
                result.ifSuccessful(::processCU)
                if (!result.isSuccessful) {
                    throw RuntimeException(result.problems.toString())
                }

                return@parseParallelized SourceRoot.Callback.Result.DONT_SAVE
            }
        }.also { println("Took $it ms") }

        println(subclassesMap.size)
        println(classToMethodImplementations.size)

        println()
    }

    private fun processCU(cu: CompilationUnit) {
        cu.findAll(ClassOrInterfaceDeclaration::class.java)
            .map { it.resolve() }
            .forEach { resolvedCU ->
                val ancestors: List<ResolvedClass> = resolvedCU.getAllAncestors(ResolvedReferenceTypeDeclaration.breadthFirstFunc)

                resolvedCU.declaredMethods.forEach { resolvedMethod: ResolvedMethod ->
                    val overriddenClasses = ancestors.filter { ancestor ->
                        ancestor.declaredMethods.any { ancestorMethod ->
                            resolvedMethod.name == ancestorMethod.name
                                && resolvedMethod.numberOfParams == ancestorMethod.noParams
                                && (0 until ancestorMethod.noParams).all { i ->
                                ancestorMethod.getParamType(i).isAssignableBy(resolvedMethod.getParam(i).type)
                            }
                        }
                    }

                    overriddenClasses.forEach { overriddenClass ->
                        classToMethodImplementations
                            .computeIfAbsent(overriddenClass) { ConcurrentHashMap() }
                            .computeIfAbsent(resolvedMethod) { Collections.synchronizedList(arrayListOf()) }
                            .add(resolvedCU)
                    }
                }

                ancestors.forEach {
                    subclassesMap.computeIfAbsent(it) { Collections.synchronizedList(arrayListOf()) }.add(resolvedCU)
                }
            }
    }
}