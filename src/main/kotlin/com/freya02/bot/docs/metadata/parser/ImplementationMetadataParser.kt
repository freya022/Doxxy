package com.freya02.bot.docs.metadata.parser

import com.github.javaparser.ast.AccessSpecifier
import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration
import com.github.javaparser.resolution.declarations.ResolvedTypeDeclaration
import com.github.javaparser.resolution.types.ResolvedReferenceType
import mu.KotlinLogging

class ImplementationMetadataParser private constructor() {
    private val cache = JavaParserCache()

    val classes: MutableMap<String, ImplementationMetadata.Class> = hashMapOf()

    fun parse(compilationUnits: List<CompilationUnit>): ImplementationMetadata {
        compilationUnits.forEachCompilationUnit(logger, ::processCU)
        parseMethodImplementations()

        cache.logCaches()

        return ImplementationMetadata(classes)
    }

    private fun processCU(cu: CompilationUnit) {
        cu.findAll(ClassOrInterfaceDeclaration::class.java)
            .map { it.resolve() }
            .forEach { resolvedCU ->
                //Add ancestors (superclasses & superinterfaces) to the map
                resolvedCU.getAllAncestorsOptimized(cache)
                    .map { it.typeDeclaration.get() }
                    // Don't process classes outside the SourceRoot, as we can't get source links for them
                    .filterNot { it.javaClass.simpleName.startsWith("Reflection") }
                    .forEach { ancestor ->
                        ancestor.metadata.subclasses.add(resolvedCU.metadata)
                        resolvedCU.metadata.superclasses.add(ancestor.metadata)
                    }
            }
    }

    //After computing all subclasses, iterate on all subclasses (the map's values),
    // take each subclass's methods and check signature compared to the superclass method (which are lower in the Set<MethodUsage>)
    private fun parseMethodImplementations() {
        //On each class, take the allMethods Set, see if a compatible method exists for each method lower in the Set
        classes.values.map { it.declaration }.forEach { subclass ->
            try {
                val allMethodsReversed = subclass.allMethodsOrdered
                    //Only keep public methods, interface methods have no access modifier but are implicitly public
                    .filter { it.cachedDeclaringType.isInterface || it.accessSpecifier() == AccessSpecifier.PUBLIC }
                    // Don't process classes outside the SourceRoot, as we can't get source links for them
                    .filterNot { it.cachedDeclaringType.javaClass.simpleName.startsWith("Reflection") }
                    .reversed()
                allMethodsReversed.forEachIndexed { i, superMethod -> //This is a super method as the list is reversed
                    val superType = superMethod.cachedDeclaringType

                    //Check for methods above in the hierarchy
                    for (j in i..<allMethodsReversed.size) { //Avoid making list copies
                        val subMethod = allMethodsReversed[j]
                        val subType = subMethod.cachedDeclaringType

                        //Some method overloads might be assignable between themselves in one way but not the other
                        // Don't compare such methods,
                        // example: SimpleLogger#debug(String, Object...) is assignable by SimpleLogger#debug(String, Object)
                        //          as Object[] is assignable to Object
                        // This also simply prevents from scanning methods from the same class lol
                        if (subType.cachedQualifiedName == superType.cachedQualifiedName)
                            continue

                        if (isMethodCompatible(cache, subMethod, superMethod)) {
                            superMethod.metadata.implementations += subMethod.metadata
                        }
                    }
                }
            } catch (e: Exception) {
                logger.error("An exception occurred while processing overrides of ${subclass.cachedQualifiedName}", e)
            }
        }
    }

    private val ResolvedReferenceTypeDeclaration.metadata: ImplementationMetadata.Class
        get() = classes.getOrPut(this.cachedQualifiedName) {
            ImplementationMetadata.Class(this, this.cachedQualifiedName)
        }

    private val ResolvedMethodDeclaration.metadata: ImplementationMetadata.Method
        get() {
            val classMetadata = cachedDeclaringType.metadata
            return classMetadata.declaredMethods.getOrPut(this.cachedSignature) {
                ImplementationMetadata.Method(this, classMetadata, this.cachedSignature)
            }
        }

    //See ResolvedReferenceTypeDeclaration#getAllMethods
    private val ResolvedReferenceTypeDeclaration.allMethodsOrdered: List<ResolvedMethodDeclaration>
        get() {
            val methods: MutableList<ResolvedMethodDeclaration> = arrayListOf()

            for (methodDeclaration in declaredMethods) {
                methods.add(methodDeclaration)
            }

            for (ancestor in allAncestors) {
                if (ancestor.isJavaLangObject) continue
                methods += ancestor.cachedLightDeclaredMethods
            }

            return methods
        }

    // Caching

    private val ResolvedTypeDeclaration.cachedQualifiedName
        get() = cache.getQualifiedName(this)

    private val ResolvedMethodDeclaration.cachedSignature
        get() = cache.getSignature(this)

    private val ResolvedReferenceType.cachedLightDeclaredMethods
        get() = cache.getLightDeclaredMethods(this)

    private val ResolvedMethodDeclaration.cachedDeclaringType
        get() = cache.getDeclaringType(this)

    companion object {
        private val logger = KotlinLogging.logger { }

        fun parseCompilationUnits(compilationUnits: List<CompilationUnit>): ImplementationMetadata {
            return ImplementationMetadataParser().parse(compilationUnits)
        }
    }
}