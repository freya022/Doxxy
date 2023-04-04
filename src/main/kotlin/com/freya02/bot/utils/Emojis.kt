package com.freya02.bot.utils

import com.freya02.botcommands.api.utils.EmojiUtils
import net.dv8tion.jda.api.entities.emoji.Emoji

object Emojis { //TODO find remaining EmojiUtils usages
    val clipboard = EmojiUtils.resolveJDAEmoji("clipboard")

    val hasImplementations = Emoji.fromFormatted("<:HasImplementations:1092833993612861481>")
    val hasOverrides = Emoji.fromFormatted("<:HasOverrides:1092833995076685886>")
    val hasSuperclasses = Emoji.fromFormatted("<:HasSuper:1092833997517758506>")

    val abstractClass = Emoji.fromFormatted("<:abstractClass_dark:1092833986583216279>")
    val annotation = Emoji.fromFormatted("<:annotationtype:1092833987950547014>")
    val `class` = Emoji.fromFormatted("<:class:1092833990085464175>")
    val enum = Emoji.fromFormatted("<:enum:1092833991272448130>")
    val `interface` = Emoji.fromFormatted("<:interface_dark:1092833999111602206>")
}