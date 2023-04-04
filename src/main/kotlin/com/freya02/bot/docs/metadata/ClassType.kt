package com.freya02.bot.docs.metadata

import com.freya02.bot.utils.Emojis
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration
import net.dv8tion.jda.api.entities.emoji.Emoji
import kotlin.jvm.optionals.getOrNull

enum class ClassType(
    val id: Int,
    val emoji: Emoji,
    private val _superDecorations: Decorations?,
    private val _subDecorations: Decorations?
) {
    CLASS(
        1,
        Emojis.`class`,
        Decorations(Emojis.hasSuperclasses, "a", "superclass", "Superclasses"),
        Decorations(Emojis.hasOverrides,  "a", "subclass", "Subclasses")
    ),
    ABSTRACT_CLASS(
        2,
        Emojis.abstractClass,
        Decorations(Emojis.hasSuperclasses, "a", "superclass", "Superclasses"),
        Decorations(Emojis.hasOverrides, "a", "subclass", "Subclasses")
    ),
    INTERFACE(
        3,
        Emojis.`interface`,
        Decorations(Emojis.hasSuperinterfaces, "a", "superinterface", "Superinterfaces"),
        Decorations(Emojis.hasImplementations, "an", "implementation", "Implementations")
    ),
    ENUM(4, Emojis.enum, null, null),
    ANNOTATION(5, Emojis.annotation, null, null);

    //TODO replace strings by localization keys
    data class Decorations(val emoji: Emoji, val article: String, val desc: String, val label: String)

    val superDecorations
        get() = _superDecorations ?: throw IllegalStateException("Cannot get super decorations for $this")

    val subDecorations
        get() = _subDecorations ?: throw IllegalStateException("Cannot get sub decorations for $this")

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