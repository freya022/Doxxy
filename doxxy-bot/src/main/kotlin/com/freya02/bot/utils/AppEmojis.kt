package com.freya02.bot.utils

import io.github.freya022.botcommands.api.emojis.AppEmojisRegistry
import io.github.freya022.botcommands.api.emojis.annotations.AppEmojiContainer

@AppEmojiContainer
object AppEmojis {

    val hasImplementations by   AppEmojisRegistry
    val hasOverrides by         AppEmojisRegistry
    val hasOverriddenMethods by AppEmojisRegistry
    val hasSuperclasses by      AppEmojisRegistry
    val hasSuperinterfaces by   AppEmojisRegistry

    val abstractClass by        AppEmojisRegistry
    val annotation by           AppEmojisRegistry
    val `class` by              AppEmojisRegistry
    val enum by                 AppEmojisRegistry
    val `interface` by          AppEmojisRegistry

    val abstractMethod by       AppEmojisRegistry
    val method by               AppEmojisRegistry

    val sync by                 AppEmojisRegistry
}