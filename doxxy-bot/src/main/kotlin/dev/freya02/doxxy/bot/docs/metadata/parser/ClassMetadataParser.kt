package dev.freya02.doxxy.bot.docs.metadata.parser

import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.ImportDeclaration
import com.github.javaparser.ast.Node
import com.github.javaparser.ast.NodeList
import com.github.javaparser.ast.body.*
import com.github.javaparser.ast.nodeTypes.NodeWithSimpleName
import com.github.javaparser.ast.type.ClassOrInterfaceType
import com.github.javaparser.ast.visitor.VoidVisitorAdapter
import com.github.javaparser.utils.SourceRoot
import dev.freya02.doxxy.bot.docs.metadata.data.ClassMetadata
import dev.freya02.doxxy.bot.docs.metadata.data.FieldMetadata
import dev.freya02.doxxy.bot.docs.metadata.data.MethodMetadata
import dev.freya02.doxxy.bot.utils.nextStep
import io.github.oshai.kotlinlogging.KotlinLogging
import org.slf4j.profiler.Profiler
import java.util.*

class ClassMetadataParser private constructor(private val sourceRoot: SourceRoot) {
    private val classMetadataMap: MutableMap<ClassName, ClassMetadata> = sortedMapOf()
    private val packageToClasses: MutableMap<String, MutableList<Pair<String, ClassName>>> = sortedMapOf()

    context(Profiler)
    fun parse(): ClassMetadataParser = this.apply {
        val apiCompilationUnits = sourceRoot.compilationUnits.filter { it.packageDeclaration.get().nameAsString.startsWith("net.dv8tion.jda.api") }

        nextStep("parsePackages") {
            apiCompilationUnits.forEachCompilationUnit(Companion.logger) { parsePackages(it) }
        }
        nextStep("parseResult") {
            apiCompilationUnits.forEachCompilationUnit(Companion.logger) { parseResult(it) }
        }
        nextStep("scanMethods") {
            apiCompilationUnits.forEachCompilationUnit(Companion.logger) { scanMethods(it) }
        }
    }

    fun getCombinedResolvedMaps(className: ClassName, map: ResolvedClassesList = hashMapOf()): ResolvedClassesList {
        val metadata = classMetadataMap[className] ?: let {
//            logger.warn("Class metadata not found for $className")
            return map
        }
        map.putAll(metadata.resolvedMap)
        metadata.extends.forEach { getCombinedResolvedMaps(it, map) }
        metadata.implements.forEach { getCombinedResolvedMaps(it, map) }
        metadata.enclosedBy?.let { getCombinedResolvedMaps(it, map) }

        return map
    }

    private fun parsePackages(compilationUnit: CompilationUnit) {
        //First add all classes to their packages so other methods can see which class can implicitly access other classes

        compilationUnit.accept(object : VoidVisitorAdapter<Void>() {
            private fun insertFullSimpleNameWithPackage(n: TypeDeclaration<*>) {
                packageToClasses.getOrPut(n.findCompilationUnit().get().packageDeclaration.get().nameAsString) {
                    arrayListOf()
                }.add(n.findSimpleFullName() to n.findCompilationUnit().get().packageDeclaration.get().nameAsString)
            }

            override fun visit(n: ClassOrInterfaceDeclaration, arg: Void?) {
                if (n.isLocalClassDeclaration) return

                insertFullSimpleNameWithPackage(n)
                super.visit(n, arg)
            }

            override fun visit(n: EnumDeclaration, arg: Void?) {
                insertFullSimpleNameWithPackage(n)
                super.visit(n, arg)
            }

            override fun visit(n: AnnotationDeclaration, arg: Void?) {
                insertFullSimpleNameWithPackage(n)
                super.visit(n, arg)
            }
        }, null)
    }

    // We need to transform source code `getPremadeWidgetHtml(Guild, WidgetTheme, int, int)` into `getPremadeWidgetHtml(Guild, WidgetUtil.WidgetTheme, int, int)`
    // so transform parameter types into their full simple name
    // There should be a pool which is a Map<[insert all FullSimpleName variants], FullSimpleName>
    // Inner classes need to be added to the pool with all their variants
    // If a type's class name cannot be found in the pool then keep the original type's class name
    private fun parseResult(compilationUnit: CompilationUnit) {
        val imports: MutableMap<FullSimpleClassName, PackageName> = hashMapOf()
        val importedVariants: MutableMap<FullSimpleClassName, FullSimpleClassName> = hashMapOf()

        imports.putAll(packageToClasses[compilationUnit.packageDeclaration.get().nameAsString]!!)

        compilationUnit.accept(object : VoidVisitorAdapter<Void>() {
            private val metadataStack: Stack<ClassMetadata> = Stack()

            private fun withClassName(n: TypeDeclaration<*>, block: () -> Unit) {
                metadataStack.push(classMetadataMap.getOrPut(n.fullyQualifiedName.get()) {
                    ClassMetadata(n.fullyQualifiedName.get(), metadataStack.lastOrNull()?.name).also {
                        it.resolvedMap.putAll(importedVariants)
                    }
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

                if (n.isAsterisk) {
                    val classes = packageToClasses[n.nameAsString] ?: let {
//                        logger.warn("Package not found for ${n.name.getPackageString()}")
                        return
                    }

                    imports.putAll(classes)
                } else {
                    val importFullSimpleClassName = n.fullClassName

                    imports[importFullSimpleClassName] = n.fullPackageName
                    findAllImportVariants(importFullSimpleClassName).forEach { variant ->
                        importedVariants[variant] = importFullSimpleClassName
                    }
                }

                super.visit(n, arg)
            }

            private fun addVariants(n: TypeDeclaration<*>) {
                val fullSimpleName = n.findSimpleFullName()
                n.findAllImportVariants().forEach {
                    metadataStack.forEach { m ->
                        val old = m.resolvedMap.put(it, fullSimpleName)
                        if (old != null) logger.warn { "Variant '$it' already existed, old value: $old, new value: $fullSimpleName" }
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

            override fun visit(n: FieldDeclaration, arg: Void?) {
                currentClassStack.peek().let { currentClass ->
                    n.variables.forEach {
                        classMetadataMap[currentClass]!!.fieldMetadataMap[it.nameAsString] =
                            FieldMetadata(n.begin.get().line..n.end.get().line)
                    }
                }

                super.visit(n, arg)
            }

            override fun visit(n: EnumConstantDeclaration, arg: Void?) {
                currentClassStack.peek().let { currentClass ->
                    classMetadataMap[currentClass]!!.fieldMetadataMap[n.nameAsString] =
                        FieldMetadata(n.begin.get().line..n.end.get().line)
                }

                super.visit(n, arg)
            }

            private fun processMethod(n: CallableDeclaration<*>) {
                currentClassStack.peek().let { currentClass ->
                    n.parameters.forEach { parameter ->
                        val typeStr = parameter.typeAsString
                        val resolvedType = when (val type = parameter.type) {
                            is ClassOrInterfaceType -> {
                                when {
                                    type.typeArguments.isEmpty -> getCombinedResolvedMaps(currentClass)[typeStr] ?: typeStr
                                    else -> {
                                        buildString {
                                            append(type.nameAsString)
                                            append("<")
                                            append(type.typeArguments.get().joinToString(", ") {
                                                getCombinedResolvedMaps(currentClass)[it.asString()] ?: it.asString()
                                            })
                                            append(">")
                                        }
                                    }
                                }
                            }
                            else -> getCombinedResolvedMaps(currentClass)[typeStr] ?: typeStr
                        }

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

    companion object {
        private val logger = KotlinLogging.logger { }

        context(Profiler)
        fun parse(sourceRoot: SourceRoot): Map<ClassName, ClassMetadata> {
            return ClassMetadataParser(sourceRoot).parse().classMetadataMap
        }
    }
}