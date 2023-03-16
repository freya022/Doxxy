package com.freya02.bot.docs.metadata

import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration
import com.github.javaparser.resolution.types.ResolvedReferenceType
import com.github.javaparser.symbolsolver.javaparsermodel.declarations.JavaParserMethodDeclaration
import mu.KotlinLogging
import java.util.*

private typealias ResolvedClass = ResolvedReferenceType
private typealias ResolvedMethod = ResolvedMethodDeclaration

class ImplementationMetadata private constructor(compilationUnits: List<CompilationUnit>) {
    // Comparators are used to determine equality, as JP instances likely do not implement hashCode/equals correctly
    private val resolvedClassComparator: Comparator<ResolvedClass> = Comparator.comparing { it.qualifiedName }
    private val resolvedMethodComparator: Comparator<ResolvedMethod> = Comparator.comparing { it.qualifiedName + it.fixedDescriptor }
    private val resolvedReferenceTypeDeclarationComparator: Comparator<ResolvedReferenceTypeDeclaration> = Comparator.comparing { it.qualifiedName }

    val subclassesMap: MutableMap<ResolvedClass, MutableList<ResolvedReferenceTypeDeclaration>> =
        Collections.synchronizedMap(TreeMap(resolvedClassComparator))

    // BaseClass -> Map<TheMethod, Set<ClassOverridingMethod>>
    val classToMethodImplementations: MutableMap<ResolvedReferenceTypeDeclaration, MutableMap<ResolvedMethod, MutableSet<ResolvedReferenceTypeDeclaration>>> =
        Collections.synchronizedMap(TreeMap(resolvedReferenceTypeDeclarationComparator))

    init {
        compilationUnits.forEachCompilationUnit(logger, ::processCU)
    }

    private fun processCU(cu: CompilationUnit) {
        cu.findAll(ClassOrInterfaceDeclaration::class.java)
            .map { it.resolve() }
            .forEach { resolvedCU ->
                val ancestors: List<ResolvedClass> = resolvedCU.getAllAncestors(ResolvedReferenceTypeDeclaration.breadthFirstFunc)

                //For the top most implementations, inherited methods may indirectly implement an interface,
                // so we need to take all methods and then check #declaringType to see where that implementation is
                resolvedCU.allMethods.map { it.declaration }.filterIsInstance<JavaParserMethodDeclaration>().forEach forEachMethod@{ resolvedMethod: ResolvedMethod ->
                    if (resolvedMethod.declaringType().qualifiedName == resolvedCU.qualifiedName) return@forEachMethod

                    //TODO Abstract methods all appear as being implement by every subclasses
                    // All inheritors of a given interface are bound to have the interface methods
                    // Figure out a way to see if the *current class* (resolvedCU) has that method
                    // This may be as simple as eliminating declaredMethods from allMethods
                    // This would also eliminate the above check
                    classToMethodImplementations
                        .computeIfAbsent(resolvedMethod.declaringType()) { Collections.synchronizedMap(TreeMap(resolvedMethodComparator)) }
                        .computeIfAbsent(resolvedMethod) { Collections.synchronizedSet(TreeSet(resolvedReferenceTypeDeclarationComparator)) }
                        .add(resolvedCU)

//                    val overriddenClasses = ancestors.filter { ancestor ->
//                        ancestor.declaredMethods.any { ancestorMethod ->
//                            resolvedMethod.name == ancestorMethod.name
//                                    && resolvedMethod.numberOfParams == ancestorMethod.noParams
//                                    && (0 until ancestorMethod.noParams).all { i ->
//                                ancestorMethod.getParamType(i).isAssignableBy(resolvedMethod.getParam(i).type)
//                            }
//                        }
//                    }
//
//                    overriddenClasses.forEach { overriddenClass ->
//                        classToMethodImplementations
//                            .computeIfAbsent(overriddenClass) { Collections.synchronizedMap(TreeMap(resolvedMethodComparator)) }
//                            .computeIfAbsent(resolvedMethod) { Collections.synchronizedSet(TreeSet(resolvedReferenceTypeDeclarationComparator)) }
//                            .add(resolvedCU) //TODO not sure if a Set is really needed, it seems to always contain a single item
//                    }
                }

                ancestors.forEach {
                    subclassesMap.computeIfAbsent(it) { Collections.synchronizedList(arrayListOf()) }.add(resolvedCU)
                }
            }
    }

    private val ResolvedMethodDeclaration.fixedDescriptor: String
        get() = buildString {
            append('(')
            for(i in 0..<numberOfParams) {
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