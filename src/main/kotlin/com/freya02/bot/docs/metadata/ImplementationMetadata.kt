package com.freya02.bot.docs.metadata

import com.github.javaparser.ast.AccessSpecifier
import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import com.github.javaparser.resolution.MethodUsage
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration
import com.github.javaparser.resolution.declarations.ResolvedTypeDeclaration
import com.github.javaparser.resolution.types.ResolvedPrimitiveType
import com.github.javaparser.resolution.types.ResolvedReferenceType
import mu.KotlinLogging
import java.util.*

private typealias ResolvedClass = ResolvedReferenceType
private typealias ResolvedMethod = ResolvedMethodDeclaration

class ImplementationMetadata private constructor(compilationUnits: List<CompilationUnit>) {
    private val cache = JavaParserCache()

    // Comparators are used to determine equality, as JP instances likely do not implement hashCode/equals correctly
    private val resolvedClassComparator: Comparator<ResolvedClass> = Comparator.comparing { it.cachedQualifiedName }
    private val resolvedMethodComparator: Comparator<ResolvedMethod> = Comparator.comparing { it.cachedQualifiedDescriptor }

    private val resolvedReferenceTypeDeclarationComparator: Comparator<ResolvedReferenceTypeDeclaration> =
        Comparator.comparing { it.cachedQualifiedName }

    val subclassesMap: MutableMap<ResolvedClass, MutableSet<ResolvedReferenceTypeDeclaration>> =
        resolvedClassComparator.createMap()

    // BaseClass -> Map<TheMethod, Set<ClassOverridingMethod>>
    val classToMethodImplementations: MutableMap<ResolvedReferenceTypeDeclaration, MutableMap<ResolvedMethod, MutableSet<ResolvedReferenceTypeDeclaration>>> =
        resolvedReferenceTypeDeclarationComparator.createMap()

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
                        subclassesMap.computeIfAbsent(it) { resolvedReferenceTypeDeclarationComparator.createSet() }
                            .add(resolvedCU)
                    }
            }

    }

    //After computing all subclasses, iterate on all superclasses (the map's keys),
    // take each subclass's methods and check signature compared to the superclass method
    private fun parseMethodImplementations() {
        //On each class, take the allMethods Set, see if a compatible method exists for each method lower in the Set
        subclassesMap.values.flatten().distinctBy { it.cachedQualifiedName }.forEach { subclass ->
            try {
                val allMethodsReversed = subclass.allMethodsOrdered
                    //Only keep public methods, interface methods have no access modifier but are implicitly public
                    .filter { it.declaringType().isInterface || it.declaration.accessSpecifier() == AccessSpecifier.PUBLIC }
                    .reversed()
                allMethodsReversed.forEachIndexed { i, superMethod -> //This is a super method as the list is reversed
                    val superType = superMethod.declaringType()

                    //Check for methods above
                    for (j in i..<allMethodsReversed.size) { //Avoid making copies
                        val subMethod = allMethodsReversed[j]
                        val subType = subMethod.declaringType()

                        //Some method overloads might be assignable between themselves in one way but not the other
                        // Don't compare such methods,
                        // example: SimpleLogger#debug(String, Object...) is assignable by SimpleLogger#debug(String, Object)
                        //          as Object[] is assignable to Object
                        // This also simply prevents from scanning methods from the same class lol
                        if (subType.cachedQualifiedName == superType.cachedQualifiedName)
                            continue

                        if (isMethodCompatible(subMethod.declaration, superMethod)) {
                            //println() //TODO should it ignore cases when superMethod is from a superclass compared to superMethod ?
                            classToMethodImplementations
                                .computeIfAbsent(superType) { resolvedMethodComparator.createMap() }
                                .computeIfAbsent(superMethod.declaration) { resolvedReferenceTypeDeclarationComparator.createSet() }
                                .add(subType)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    //See ResolvedReferenceTypeDeclaration#getAllMethods
    private val ResolvedReferenceTypeDeclaration.allMethodsOrdered: List<MethodUsage>
        get() {
            val methods: MutableList<MethodUsage> = arrayListOf()

            for (methodDeclaration in declaredMethods) {
                val methodUsage = MethodUsage(methodDeclaration)
                methods.add(methodUsage)
            }

            for (ancestor in allAncestors) {
                if (ancestor.isJavaLangObject) continue

                val typeParametersMap = ancestor.typeParametersMap
                for (mu in ancestor.cachedDeclaredMethods) {
                    // replace type parameters to be able to filter away overridden generified methods
                    var methodUsage = mu
                    for (p in typeParametersMap) {
                        methodUsage = methodUsage.replaceTypeParameter(p.a, p.b)
                    }

                    methods.add(mu)
                }
            }

            return methods
        }

    private fun isMethodCompatible(subMethod: ResolvedMethodDeclaration, superMethod: MethodUsage): Boolean {
        if (subMethod.name != superMethod.name) return false
        if (subMethod.numberOfParams != superMethod.noParams) return false

        return (0 until superMethod.noParams).all { i ->
            when (val superType = superMethod.getParamType(i)) {
                //JP says that converting an int to a long is possible, but what we want is to check the types
                // Check primitive name if it is one instead
                is ResolvedPrimitiveType -> subMethod.getParam(i).type.isPrimitive
                        && superType.describe() == subMethod.getParam(i).type.asPrimitive().describe()

                else -> superType.isAssignableBy(subMethod.getParam(i).type)
            }
        }
    }

    private fun <K, V> Comparator<K>.createMap(): MutableMap<K, V> = Collections.synchronizedMap(TreeMap(this))
    private fun <E> Comparator<E>.createSet(): MutableSet<E> = Collections.synchronizedSet(TreeSet(this))

    private val ResolvedTypeDeclaration.cachedQualifiedName
        get() = cache.getQualifiedName(this)

    private val ResolvedReferenceType.cachedQualifiedName
        get() = cache.getQualifiedName(this)

    private val ResolvedMethodDeclaration.cachedQualifiedDescriptor
        get() = cache.getQualifiedDescriptor(this)

    private val ResolvedReferenceType.cachedDeclaredMethods
        get() = cache.getDeclaredMethods(this)

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

        fun <T> Map<T, Iterable<ResolvedReferenceTypeDeclaration>>.flattenReferences() =
            map { (k, v) -> v.map { it.qualifiedName } }.flatten()

        fun fromCompilationUnits(compilationUnits: List<CompilationUnit>): ImplementationMetadata {
            return ImplementationMetadata(compilationUnits)
        }
    }
}