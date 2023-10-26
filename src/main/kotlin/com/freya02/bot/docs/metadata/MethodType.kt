package com.freya02.bot.docs.metadata

import com.freya02.bot.utils.Emojis
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration
import io.github.freya022.botcommands.api.core.BContext
import io.github.freya022.botcommands.api.core.service.getService
import io.github.freya022.botcommands.api.localization.LocalizationService
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

    class Decorations private constructor(
        val emoji: EmojiUnion,
        private val labelKey: String,
        private val titleKey: String,
        private val placeholderKey: String
    ) {
        fun getLabel(context: BContext): String = getLocalization(context, labelKey)
        fun getTitle(context: BContext): String = getLocalization(context, titleKey)
        fun getPlaceholder(context: BContext): String = getLocalization(context, placeholderKey)

        private fun getLocalization(context: BContext, key: String): String {
            val localization = context.getService<LocalizationService>().getInstance("Docs", Locale.ROOT)
                ?: throw IllegalStateException("Could not get decorations localization bundle (Docs)")
            return localization[key]?.localize()
                ?: throw IllegalArgumentException("Could not get localization for $key")
        }

        companion object {
            /**
             * @param classTypeKey declaration/definition
             * @param hierarchicalName impl/super
             */
            fun fromLocalizations(emoji: EmojiUnion, classTypeKey: String, hierarchicalName: String): Decorations {
                return Decorations(
                    emoji,
                    "method_type_decorations.$classTypeKey.$hierarchicalName.${"links_button_label"}",
                    "method_type_decorations.$classTypeKey.$hierarchicalName.${"links_embed_title"}",
                    "method_type_decorations.$classTypeKey.$hierarchicalName.${"links_select_placeholder"}"
                )
            }
        }
    }

    companion object {
        fun fromDeclaration(declaration: ResolvedMethodDeclaration): MethodType = when {
            declaration.isAbstract -> DECLARATION
            else -> DEFINITION
        }

        fun fromId(id: Int): MethodType = entries.first { it.id == id }
    }
}