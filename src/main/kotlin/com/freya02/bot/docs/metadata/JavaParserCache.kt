package com.freya02.bot.docs.metadata

import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration
import com.github.javaparser.resolution.declarations.ResolvedTypeDeclaration
import com.github.javaparser.resolution.types.ResolvedReferenceType
import java.util.*

class JavaParserCache {
    //JP keeps recreating these map's keys,
    // but these keys are still being used multiple times to a point where it is still beneficial
    // An IdentityHashMap is used as JP *might* not implement hashCode/equals correctly, while using a property as a key would be counterproductive
    private val typeDeclarationQualifiedNames: MutableMap<ResolvedTypeDeclaration, String> = Collections.synchronizedMap(IdentityHashMap())
    private val referenceTypeQualifiedNames: MutableMap<ResolvedReferenceType, String> = Collections.synchronizedMap(IdentityHashMap())
    private val methodDeclarationQualifiedDescriptors: MutableMap<ResolvedMethodDeclaration, String> = Collections.synchronizedMap(IdentityHashMap())

    fun getQualifiedName(declaration: ResolvedTypeDeclaration): String {
        return typeDeclarationQualifiedNames.getOrPut(declaration) { declaration.qualifiedName }
    }

    fun getQualifiedName(declaration: ResolvedReferenceType): String {
        return referenceTypeQualifiedNames.getOrPut(declaration) { declaration.qualifiedName }
    }

    fun getQualifiedDescriptor(declaration: ResolvedMethodDeclaration): String {
        return methodDeclarationQualifiedDescriptors.getOrPut(declaration) { declaration.qualifiedName + declaration.fixedDescriptor }
    }

    private val ResolvedMethodDeclaration.fixedDescriptor: String
        get() = buildString {
            append('(')
            for (i in 0..<numberOfParams) {
                append(getParam(i).type.describe())
            }
            append(')')

            append(returnType.describe())
        }
}