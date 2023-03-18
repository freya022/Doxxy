package com.freya02.bot.docs.metadata

import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration

class ImplementationMetadata(
    val subclassesMap: Map<ResolvedClass, Set<ResolvedReferenceTypeDeclaration>>,
    // BaseClass -> Map<TheMethod, Set<ClassOverridingMethod>>
    val classToMethodImplementations: Map<ResolvedReferenceTypeDeclaration, Map<ResolvedMethod, Set<ResolvedReferenceTypeDeclaration>>>
)