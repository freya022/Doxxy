package com.freya02.bot.docs.metadata

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration
import kotlin.jvm.optionals.getOrNull

enum class ClassType(val id: Int) {
    CLASS(1),
    ABSTRACT_CLASS(2),
    INTERFACE(3),
    ENUM(4),
    ANNOTATION(5);

    companion object {
        fun fromDeclaration(declaration: ResolvedReferenceTypeDeclaration): ClassType {
            if (declaration.isClass) {
                val astNode = declaration.toAst(ClassOrInterfaceDeclaration::class.java).getOrNull()
                return when (astNode?.isAbstract) {
                    true -> ABSTRACT_CLASS
                    else -> CLASS
                }
            }
            if (declaration.isInterface) return INTERFACE
            if (declaration.isEnum) return ENUM
            if (declaration.isAnnotation) return ANNOTATION

            throw IllegalArgumentException("Unknown class type: $declaration")
        }

        fun fromId(id: Int): ClassType = values().first { it.id == id }
    }
}