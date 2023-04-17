package com.freya02.bot.docs.metadata

import com.freya02.bot.utils.Emojis
import com.freya02.botcommands.api.localization.Localization
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.entities.emoji.EmojiUnion
import java.util.*
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
        Decorations.fromLocalizations(Emojis.hasSuperclasses, "class", "super"),
        Decorations.fromLocalizations(Emojis.hasOverrides, "class", "sub")
    ),
    ABSTRACT_CLASS(
        2,
        Emojis.abstractClass,
        Decorations.fromLocalizations(Emojis.hasSuperclasses, "class", "super"),
        Decorations.fromLocalizations(Emojis.hasOverrides, "class", "sub")
    ),
    INTERFACE(
        3,
        Emojis.`interface`,
        Decorations.fromLocalizations(Emojis.hasSuperinterfaces, "interface", "super"),
        Decorations.fromLocalizations(Emojis.hasImplementations, "interface", "sub")
    ),
    ENUM(4, Emojis.enum, null, null),
    ANNOTATION(5, Emojis.annotation, null, null);

    class Decorations private constructor(val emoji: EmojiUnion, val label: String, val title: String, val placeholder: String) {
        companion object {
            private val localization = Localization.getInstance("Docs", Locale.ROOT)
                ?: throw IllegalStateException("Could not get decorations localization bundle (Docs)")

            private fun getLocalization(classTypeKey: String, hierarchicalName: String, finalKey: String): String {
                val key = "class_type_decorations.$classTypeKey.$hierarchicalName.$finalKey"

                return localization[key]?.localize()
                    ?: throw IllegalArgumentException("Could not get localization for $key")
            }

            /**
             * @param classTypeKey class/interface
             * @param hierarchicalName super/sub
             */
            fun fromLocalizations(emoji: EmojiUnion, classTypeKey: String, hierarchicalName: String): Decorations {
                return Decorations(
                    emoji,
                    getLocalization(classTypeKey, hierarchicalName, "links_button_label"),
                    getLocalization(classTypeKey, hierarchicalName, "links_embed_title"),
                    getLocalization(classTypeKey, hierarchicalName, "links_select_placeholder")
                )
            }
        }
    }

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