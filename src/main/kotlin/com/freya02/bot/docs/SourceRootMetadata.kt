package com.freya02.bot.docs

import com.freya02.botcommands.api.Logging
import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.ImportDeclaration
import com.github.javaparser.ast.NodeList
import com.github.javaparser.ast.body.*
import com.github.javaparser.ast.visitor.VoidVisitorAdapter
import com.github.javaparser.utils.SourceRoot
import java.nio.file.Path
import java.util.*

private typealias ClassName = String
private typealias FullSimpleClassName = String
private typealias MethodParameters = String
private typealias ResolvedClassesList = MutableMap<FullSimpleClassName, FullSimpleClassName>
private typealias MethodMap = MutableMap<String, MutableList<MethodParameters>>

class SourceRootMetadata(sourceRootPath: Path) {
    private val logger = Logging.getLogger()
    private val sourceRoot: SourceRoot = SourceRoot(sourceRootPath)

    private val map: MutableMap<ClassName, MethodMap> = sortedMapOf()

    init {
        sourceRoot.parse("net.dv8tion.jda.api") { localPath, _, result ->
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
    }

    fun getMethodsParameters(className: ClassName, methodName: String): List<MethodParameters> {
        return map[className]
            ?.get(methodName)
            ?: emptyList()
    }

    // We need to transform source code `getPremadeWidgetHtml(Guild, WidgetTheme, int, int)` into `getPremadeWidgetHtml(Guild, WidgetUtil.WidgetTheme, int, int)`
    // so transform parameter types into their full simple name
    // There should be a pool which is a Map<[insert all FullSimpleName variants], FullSimpleName>
    // Inner classes need to be added to the pool with all their variants
    // If a type's class name cannot be found in the pool then keep the original type's class name
    private fun parseResult(compilationUnit: CompilationUnit) {
//        val imports: MutableList<String> = arrayListOf()
        val resolvedMap: ResolvedClassesList = sortedMapOf()

        compilationUnit.accept(object : VoidVisitorAdapter<Void>() {
            override fun visit(n: ClassOrInterfaceDeclaration, arg: Void?) {
                if (!n.isPublic && !n.isProtected) return
                if (n.isLocalClassDeclaration) return

                addVariants(n)
                super.visit(n, arg)
            }

            override fun visit(n: EnumDeclaration, arg: Void?) {
                if (!n.isPublic && !n.isProtected) return

                addVariants(n)
                super.visit(n, arg)
            }

            override fun visit(n: AnnotationDeclaration, arg: Void?) {
                if (!n.isPublic && !n.isProtected) return

                addVariants(n)
                super.visit(n, arg)
            }

            override fun visit(n: ImportDeclaration, arg: Void?) {
                if (n.isStatic) return
                if (n.isAsterisk) return

//                imports.add(n.name.identifier)
//                super.visit(n, arg)
            }

            private fun addVariants(n: TypeDeclaration<*>) {
                val fullSimpleName = n.findSimpleFullName()
                n.findAllImportVariants().forEach {
                    val old = resolvedMap.put(it, fullSimpleName)
                    if (old != null) logger.warn("Variant '$it' already existed, old value: $old, new value: $fullSimpleName")
                }
            }
        }, null)

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

            override fun visit(n: MethodDeclaration, arg: Void?) {
                currentClassStack.peek().let { currentClass ->
                    n.parameters.forEach { parameter ->
                        val typeStr = parameter.typeAsString
                        val resolvedType = resolvedMap[typeStr] ?: typeStr

                        parameter.setType(resolvedType)
                    }

                    map
                        .getOrPut(currentClass) { hashMapOf() }
                        .getOrPut(n.nameAsString) { arrayListOf() }
                        .add(n.parameters.toSimpleParameterString())
                }

                super.visit(n, arg)
            }
        }, null)
    }

    private fun NodeList<Parameter>.toSimpleParameterString(): String = joinToString(", ") { "${it.typeAsString} ${it.nameAsString}" }

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