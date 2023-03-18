package com.freya02.bot.docs.metadata

import com.github.javaparser.ast.AccessSpecifier
import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import com.github.javaparser.resolution.MethodUsage
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration
import com.github.javaparser.resolution.declarations.ResolvedTypeDeclaration
import com.github.javaparser.resolution.types.ResolvedReferenceType
import mu.KotlinLogging

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
                    .filter { it.cachedDeclaringType.isInterface || it.declaration.accessSpecifier() == AccessSpecifier.PUBLIC }
                    .reversed()
                allMethodsReversed.forEachIndexed { i, superMethod -> //This is a super method as the list is reversed
                    val superType = superMethod.cachedDeclaringType

                    //Check for methods above
                    for (j in i..<allMethodsReversed.size) { //Avoid making copies
                        val subMethod = allMethodsReversed[j]
                        val subType = subMethod.cachedDeclaringType

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

    private val ResolvedTypeDeclaration.cachedQualifiedName
        get() = cache.getQualifiedName(this)

    private val ResolvedReferenceType.cachedQualifiedName
        get() = cache.getQualifiedName(this)

    private val ResolvedMethodDeclaration.cachedQualifiedDescriptor
        get() = cache.getQualifiedDescriptor(this)

    private val ResolvedReferenceType.cachedDeclaredMethods
        get() = cache.getDeclaredMethods(this)

    private val MethodUsage.cachedDeclaringType
        get() = cache.getDeclaringType(this)

    companion object {
        private val logger = KotlinLogging.logger { }

        fun fromCompilationUnits(compilationUnits: List<CompilationUnit>): ImplementationMetadata {
            return ImplementationMetadata(compilationUnits)
        }
    }
}