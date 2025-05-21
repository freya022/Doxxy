package dev.freya02.doxxy.bot.docs.metadata.parser

import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.ImportDeclaration
import com.github.javaparser.ast.Node
import com.github.javaparser.ast.body.*
import com.github.javaparser.ast.nodeTypes.NodeWithRange
import com.github.javaparser.ast.nodeTypes.NodeWithSimpleName
import com.github.javaparser.ast.nodeTypes.NodeWithTypeArguments
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

    context(profiler: Profiler)
    fun parse(): ClassMetadataParser = this.apply {
        val apiCompilationUnits = sourceRoot.compilationUnits.filter {
            val pkgName = it.packageDeclaration.get().nameAsString
            pkgName.startsWith("net.dv8tion.jda.api") || pkgName.startsWith("net.dv8tion.jda.annotations")
        }

        profiler.nextStep("parsePackages") {
            apiCompilationUnits.forEachCompilationUnit(Companion.logger) { parsePackages(it) }
        }
        profiler.nextStep("parseResult") {
            apiCompilationUnits.forEachCompilationUnit(Companion.logger) { parseResult(it) }
        }
        profiler.nextStep("scanMethods") {
            apiCompilationUnits.forEachCompilationUnit(Companion.logger) { scanMethods(it) }
        }
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
                    ClassMetadata(n.fullyQualifiedName.get(), n.rangeKt, metadataStack.lastOrNull()?.name).also {
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
                            FieldMetadata(n.rangeKt)
                    }
                }

                super.visit(n, arg)
            }

            override fun visit(n: EnumConstantDeclaration, arg: Void?) {
                currentClassStack.peek().let { currentClass ->
                    classMetadataMap[currentClass]!!.fieldMetadataMap[n.nameAsString] =
                        FieldMetadata(n.rangeKt)
                }

                super.visit(n, arg)
            }

            override fun visit(n: AnnotationMemberDeclaration, arg: Void?) {
                currentClassStack.peek().also { currentClass ->
                    val classMetadata = classMetadataMap[currentClass]!!
                    check(n.nameAsString !in classMetadata.methodMetadataMap) {
                        "Annotation member '$currentClass#${n.nameAsString}' already exists"
                    }

                    classMetadata.methodMetadataMap[n.nameAsString] = arrayListOf(
                        MethodMetadata(
                            emptyList(),
                            n.rangeKt,
                        )
                    )
                }

                super.visit(n, arg)
            }

            private fun processMethod(n: CallableDeclaration<*>) {
                currentClassStack.peek().also { currentClass ->
                    n.parameters.map { it.type }
                        .filterIsInstance<NodeWithTypeArguments<*>>()
                        .forEach { it.removeTypeArguments() }

                    classMetadataMap[currentClass]!!
                        .methodMetadataMap
                        .getOrPut(n.nameAsString) { arrayListOf() }
                        .add(
                            MethodMetadata(
                                n.parameters.map { it.resolve().describeType() },
                                n.rangeKt,
                            )
                        )
                }
            }
        }, null)
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

        context(_: Profiler)
        fun parse(sourceRoot: SourceRoot): Map<ClassName, ClassMetadata> {
            return ClassMetadataParser(sourceRoot).parse().classMetadataMap
        }
    }
}

private val NodeWithRange<*>.rangeKt: IntRange
    get() {
        val range = range.get()
        return range.begin.line..range.end.line
    }