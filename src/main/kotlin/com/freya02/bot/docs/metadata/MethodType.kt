package com.freya02.bot.docs.metadata

import com.freya02.bot.utils.Emojis
import com.freya02.botcommands.api.localization.Localization
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.entities.emoji.EmojiUnion
import java.util.*

enum class MethodType(
    val id: Int,
    val emoji: Emoji,
    val implementationDecorations: Decorations,
    val overriddenMethodsDecorations: Decorations
) {
    DECLARATION(
        1,
        Emojis.methodDeclaration,
        Decorations.fromLocalizations(Emojis.hasImplementations, "declaration", "impl"),
        Decorations.fromLocalizations(Emojis.hasOverriddenMethods, "declaration", "overriddenMethods")
    ),
    DEFINITION(
        2,
        Emojis.methodDefinition,
        Decorations.fromLocalizations(Emojis.hasOverrides, "definition", "impl"),
        Decorations.fromLocalizations(Emojis.hasOverriddenMethods, "definition", "overriddenMethods")
    );

    class Decorations private constructor(val emoji: EmojiUnion, val label: String, val title: String, val placeholder: String) {
        companion object {
            private val localization = Localization.getInstance("Docs", Locale.ROOT)
                ?: throw IllegalStateException("Could not get decorations localization bundle (Docs)")

            private fun getLocalization(classTypeKey: String, hierarchicalName: String, finalKey: String): String {
                val key = "method_type_decorations.$classTypeKey.$hierarchicalName.$finalKey"

                return localization[key]?.localize()
                    ?: throw IllegalArgumentException("Could not get localization for $key")
            }

            /**
             * @param classTypeKey declaration/definition
             * @param hierarchicalName impl/super
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

    companion object {
        fun fromDeclaration(declaration: ResolvedMethodDeclaration): MethodType = when {
            declaration.isAbstract -> DECLARATION
            else -> DEFINITION
        }

        fun fromId(id: Int): MethodType = values().first { it.id == id }
    }
}