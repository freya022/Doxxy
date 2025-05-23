package dev.freya02.doxxy.bot.docs.metadata

import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration
import dev.freya02.doxxy.bot.utils.AppEmojis
import io.github.freya022.botcommands.api.core.BContext
import io.github.freya022.botcommands.api.core.service.getService
import io.github.freya022.botcommands.api.localization.LocalizationService
import net.dv8tion.jda.api.entities.emoji.CustomEmoji
import net.dv8tion.jda.api.entities.emoji.Emoji
import java.util.*

enum class MethodType(
    val id: Int,
    val emoji: Emoji,
    val implementationDecorations: Decorations,
    val overriddenMethodsDecorations: Decorations
) {
    DECLARATION(
        1,
        AppEmojis.abstractMethod,
        Decorations.fromLocalizations(AppEmojis.hasImplementations, "declaration", "impl"),
        Decorations.fromLocalizations(AppEmojis.hasOverriddenMethods, "declaration", "overriddenMethods")
    ),
    DEFINITION(
        2,
        AppEmojis.method,
        Decorations.fromLocalizations(AppEmojis.hasOverrides, "definition", "impl"),
        Decorations.fromLocalizations(AppEmojis.hasOverriddenMethods, "definition", "overriddenMethods")
    );

    class Decorations private constructor(
        val emoji: CustomEmoji,
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
            fun fromLocalizations(emoji: CustomEmoji, classTypeKey: String, hierarchicalName: String): Decorations {
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