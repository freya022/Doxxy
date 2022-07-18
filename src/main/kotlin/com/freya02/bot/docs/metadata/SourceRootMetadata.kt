package com.freya02.bot.docs.metadata

import com.freya02.botcommands.api.Logging
import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.ImportDeclaration
import com.github.javaparser.ast.Node
import com.github.javaparser.ast.NodeList
import com.github.javaparser.ast.body.*
import com.github.javaparser.ast.nodeTypes.NodeWithSimpleName
import com.github.javaparser.ast.visitor.VoidVisitorAdapter
import com.github.javaparser.utils.SourceRoot
import java.nio.file.Path
import java.util.*

class SourceRootMetadata(sourceRootPath: Path) {
    private val logger = Logging.getLogger()
    private val sourceRoot: SourceRoot = SourceRoot(sourceRootPath)

    private val classMetadataMap: MutableMap<ClassName, ClassMetadata> = sortedMapOf()

    init {
        sourceRoot.parse("net.dv8tion.jda.api") { localPath, _, result ->
//        sourceRoot.parse("net.dv8tion.jda.api") { localPath, _, result ->
            if (result.problems.isNotEmpty()) {
                result.problems.forEach { logger.error(it.toString()) }
            }
            result.ifSuccessful {
                kotlin.runCatching { parseResult(it) }.onFailure {
                    logger.error("Failed to parse $localPath", it)
                }
            }
            SourceRoot.Callback.Result.DONT_SAVE
        }

        sourceRoot.parse("net.dv8tion.jda.api") { localPath, _, result ->
//        sourceRoot.parse("net.dv8tion.jda.api") { localPath, _, result ->
            if (result.problems.isNotEmpty()) {
                result.problems.forEach { logger.error(it.toString()) }
            }
            result.ifSuccessful {
                kotlin.runCatching { scanMethods(it) }.onFailure {
                    logger.error("Failed to parse method of $localPath", it)
                }
            }
            SourceRoot.Callback.Result.DONT_SAVE
        }
    }

    fun getMethodsParameters(className: ClassName, methodName: String): List<MethodMetadata> {
        return emptyList()
    }

    fun getCombinedResolvedMaps(className: ClassName, map: ResolvedClassesList = hashMapOf()): ResolvedClassesList {
        val metadata = classMetadataMap[className] ?: let {
            logger.warn("Class metadata not found for $className")
            return map
        }
        map.putAll(metadata.resolvedMap)
        metadata.extends.forEach { getCombinedResolvedMaps(it, map) }
        metadata.implements.forEach { getCombinedResolvedMaps(it, map) }
        metadata.enclosedBy?.let { getCombinedResolvedMaps(it, map) }

        return map
    }

    //TODO inherited inner classes should be accessible

    //TODO do a service where simple names can be resolved to package + name in certain compilation units ?

    // We need to transform source code `getPremadeWidgetHtml(Guild, WidgetTheme, int, int)` into `getPremadeWidgetHtml(Guild, WidgetUtil.WidgetTheme, int, int)`
    // so transform parameter types into their full simple name
    // There should be a pool which is a Map<[insert all FullSimpleName variants], FullSimpleName>
    // Inner classes need to be added to the pool with all their variants
    // If a type's class name cannot be found in the pool then keep the original type's class name
    private fun parseResult(compilationUnit: CompilationUnit) {
        val imports: MutableMap<String, String> = hashMapOf()

        compilationUnit.accept(object : VoidVisitorAdapter<Void>() {
            private val metadataStack: Stack<ClassMetadata> = Stack()

            private fun withClassName(n: TypeDeclaration<*>, block: () -> Unit) {
                metadataStack.push(classMetadataMap.getOrPut(n.fullyQualifiedName.get()) {
                    ClassMetadata(n.fullyQualifiedName.get(), metadataStack.lastOrNull()?.name)
                })
                block()
                metadataStack.pop()
            }

            private fun <T> resolveWithImports(it: T): String
                    where T : Node,
                          T : NodeWithSimpleName<*> {
                return imports.getOrElse(it.nameAsString) {
                    it.findCompilationUnit().get().packageDeclaration.get().nameAsString
                } + "." + it.nameAsString
            }

            override fun visit(n: ClassOrInterfaceDeclaration, arg: Void?) {
                if (n.isLocalClassDeclaration) return

                withClassName(n) {
                    addVariants(n)
                    metadataStack.peek().extends.addAll(n.extendedTypes.map { resolveWithImports(it) })
                    metadataStack.peek().implements.addAll(n.implementedTypes.map { resolveWithImports(it) })
                    super.visit(n, arg)
                }
            }

            override fun visit(n: EnumDeclaration, arg: Void?) {
                withClassName(n) {
                    addVariants(n)
                    metadataStack.peek().implements.addAll(n.implementedTypes.map { resolveWithImports(it) })
                    super.visit(n, arg)
                }
            }

            override fun visit(n: AnnotationDeclaration, arg: Void?) {
                withClassName(n) {
                    addVariants(n)
                    super.visit(n, arg)
                }
            }

            override fun visit(n: ImportDeclaration, arg: Void?) {
                if (n.isStatic) return
                if (n.isAsterisk) return

//                val fullSimpleName = n.name.identifier
//                findAllImportVariants(fullSimpleName).forEach {
//                    val old = resolvedMap.put(it, fullSimpleName)
//                    if (old != null) logger.warn("Variant '$it' already existed, old value: $old, new value: $fullSimpleName")
//                }

                imports[n.name.identifier] = n.name.qualifier.get().asString()
                super.visit(n, arg)
            }

            private fun addVariants(n: TypeDeclaration<*>) {
                val fullSimpleName = n.findSimpleFullName()
                n.findAllImportVariants().forEach {
                    metadataStack.forEach { m ->
                        val old = m.resolvedMap.put(it, fullSimpleName)
                        if (old != null) logger.warn("Variant '$it' already existed, old value: $old, new value: $fullSimpleName")
                    }
                }
            }
        }, null)
    }

    private fun scanMethods(compilationUnit: CompilationUnit) {
        compilationUnit.accept(object : VoidVisitorAdapter<Void>() {
            val currentClassStack: Stack<String> = Stack()

            private fun withClassName(n: TypeDeclaration<*>, block: () -> Unit) {
                currentClassStack.push(n.fullyQualifiedName.get())
                block()
                currentClassStack.pop()
            }

            override fun visit(n: ClassOrInterfaceDeclaration, arg: Void?) {
                if (n.isLocalClassDeclaration) return

                withClassName(n) {
                    super.visit(n, arg)
                }
            }

            override fun visit(n: EnumDeclaration, arg: Void?) {
                withClassName(n) {
                    super.visit(n, arg)
                }
            }

            override fun visit(n: AnnotationDeclaration, arg: Void?) {
                withClassName(n) {
                    super.visit(n, arg)
                }
            }

            override fun visit(n: ConstructorDeclaration, arg: Void?) {
                processMethod(n)
                super.visit(n, arg)
            }

            override fun visit(n: MethodDeclaration, arg: Void?) {
                processMethod(n)
                super.visit(n, arg)
            }

            private fun processMethod(n: CallableDeclaration<*>) {
                currentClassStack.peek().let { currentClass ->
                    n.parameters.forEach { parameter ->
                        val typeStr = parameter.typeAsString
                        val resolvedType = getCombinedResolvedMaps(currentClass)[typeStr] ?: typeStr

                        parameter.setType(resolvedType)
                    }

                    return@let classMetadataMap[currentClass]!!
                        .methodMetadataMap
                        .getOrPut(n.nameAsString) { arrayListOf() }
                        .add(
                            MethodMetadata(
                                n.parameters.toSimpleParameterString(),
                                n.begin.get().line..n.end.get().line
                            )
                        )
                }
            }
        }, null)
    }

    private fun getMasterKey(compilationUnit: CompilationUnit) =
        "${compilationUnit.packageDeclaration.get().nameAsString}.${compilationUnit.primaryTypeName.get()}"

    private fun NodeList<Parameter>.toSimpleParameterString(): String = joinToString(", ") {
        when {
            it.isVarArgs -> "${it.typeAsString}... ${it.nameAsString}"
            else -> "${it.typeAsString} ${it.nameAsString}"
        }
    }

    private fun findAllImportVariants(simpleFullName: String): List<String> {
        val split = simpleFullName.split('.')

        return List(split.size) { i ->
            split.takeLast(i + 1).joinToString(".")
        }
    }

    private fun TypeDeclaration<*>.findAllImportVariants(): List<String> {
        val split = findSimpleFullName().split('.')

        return List(split.size) { i ->
            split.takeLast(i + 1).joinToString(".")
        }
    }

    private fun TypeDeclaration<*>.findSimpleFullName(): String {
        if (isTopLevelType) {
            return nameAsString
        }

        return findAncestor(TypeDeclaration::class.java).get().findSimpleFullName() + "." + nameAsString
    }
}