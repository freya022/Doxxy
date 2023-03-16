package com.freya02.bot.docs.metadata

import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import com.github.javaparser.resolution.MethodUsage
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration
import com.github.javaparser.resolution.types.ResolvedReferenceType
import mu.KotlinLogging
import java.util.*

private typealias ResolvedClass = ResolvedReferenceType
private typealias ResolvedMethod = ResolvedMethodDeclaration

class ImplementationMetadata private constructor(compilationUnits: List<CompilationUnit>) {
    // Comparators are used to determine equality, as JP instances likely do not implement hashCode/equals correctly
    private val resolvedClassComparator: Comparator<ResolvedClass> = Comparator.comparing { it.qualifiedName }
    private val resolvedMethodComparator: Comparator<ResolvedMethod> =
        Comparator.comparing { it.qualifiedName + it.fixedDescriptor }
    private val resolvedReferenceTypeDeclarationComparator: Comparator<ResolvedReferenceTypeDeclaration> =
        Comparator.comparing { it.qualifiedName }

    val subclassesMap: MutableMap<ResolvedClass, MutableList<ResolvedReferenceTypeDeclaration>> =
        Collections.synchronizedMap(TreeMap(resolvedClassComparator))

    // BaseClass -> Map<TheMethod, Set<ClassOverridingMethod>>
    val classToMethodImplementations: MutableMap<ResolvedClass, MutableMap<ResolvedMethod, MutableSet<ResolvedReferenceTypeDeclaration>>> =
        Collections.synchronizedMap(TreeMap(resolvedClassComparator))

    init {
        compilationUnits.forEachCompilationUnit(logger, ::processCU)
        parseMethodImplementations()
    }

    private fun processCU(cu: CompilationUnit) {
        cu.findAll(ClassOrInterfaceDeclaration::class.java)
            .map { it.resolve() }
            .forEach { resolvedCU ->
                //Add ancestors (superclasses & superinterfaces) to the map
                resolvedCU
                    .getAllAncestors(ResolvedReferenceTypeDeclaration.breadthFirstFunc)
                    .filterNot { it.isJavaLangEnum || it.isJavaLangObject }
                    .forEach {
                        subclassesMap.computeIfAbsent(it) { Collections.synchronizedList(arrayListOf()) }
                            .add(resolvedCU)
                    }
            }

    }

    //After computing all subclasses, iterate on all superclasses (the map's keys),
    // take each subclass's methods and check signature compared to the superclass method
    private fun parseMethodImplementations() {
        subclassesMap.forEach { (superclass, subclasses) ->
            superclass.declaredMethods.forEach { superMethod ->
                subclasses.forEach { subclass ->
                    subclass.declaredMethods.forEach { subMethod ->
                        if (isMethodCompatible(subMethod, superMethod)) {
                            classToMethodImplementations
                                .computeIfAbsent(superclass) { resolvedMethodComparator.createMap() }
                                .computeIfAbsent(superMethod.declaration) { resolvedReferenceTypeDeclarationComparator.createSet() }
                                .add(subclass)
                        }
                    }
                }
            }
        }
    }

    private fun isMethodCompatible(subMethod: ResolvedMethodDeclaration, superMethod: MethodUsage): Boolean {
        if (subMethod.name != superMethod.name) return false
        if (subMethod.numberOfParams != superMethod.noParams) return false

        return (0 until superMethod.noParams).all { i ->
            superMethod.getParamType(i).isAssignableBy(subMethod.getParam(i).type)
        }
    }

    private fun <K, V> Comparator<K>.createMap(): MutableMap<K, V> = Collections.synchronizedMap(TreeMap(this))
    private fun <E> Comparator<E>.createSet(): MutableSet<E> = Collections.synchronizedSet(TreeSet(this))

    private val ResolvedMethodDeclaration.fixedDescriptor: String
        get() = buildString {
            append('(')
            for (i in 0..<numberOfParams) {
                append(getParam(i).type.describe())
            }
            append(')')

            append(returnType.describe())
        }

    companion object {
        private val logger = KotlinLogging.logger { }

        fun <T> Map<ResolvedReferenceType, T>.findRefByClassName(name: String): T {
            return this.toList().first { (k, _) -> k.qualifiedName.endsWith(".$name") }.second
        }

        fun <T> Map<ResolvedReferenceTypeDeclaration, T>.findDeclByClassName(name: String): T {
            return this.toList().first { (k, _) -> k.qualifiedName.endsWith(".$name") }.second
        }

        fun <T> Map<ResolvedMethod, T>.findByMethodName(name: String): Map<String, T> {
            return this.filterKeys { it.name == name }.mapKeys { (k, _) -> k.className }
        }

        fun fromCompilationUnits(compilationUnits: List<CompilationUnit>): ImplementationMetadata {
            return ImplementationMetadata(compilationUnits)
        }
    }
}