package com.freya02.bot.docs.metadata

import com.freya02.bot.utils.Emojis
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration
import net.dv8tion.jda.api.entities.emoji.Emoji

enum class MethodType(
    val id: Int,
    val emoji: Emoji
) {
    DECLARATION(
        1,
        Emojis.methodDeclaration
    ),
    DEFINITION(
        2,
        Emojis.methodDefinition
    );

    companion object {
        fun fromDeclaration(declaration: ResolvedMethodDeclaration): MethodType = when {
            declaration.isAbstract -> DEFINITION
            else -> DECLARATION
        }

        fun fromId(id: Int): MethodType = values().first { it.id == id }
    }
}