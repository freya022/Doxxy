package com.freya02.bot.utils

import com.freya02.botcommands.api.utils.EmojiUtils
import net.dv8tion.jda.api.entities.emoji.Emoji

object Emojis { //TODO find remaining EmojiUtils usages
    val clipboard = EmojiUtils.resolveJDAEmoji("clipboard")

    //TODO maybe center emojis ?
    //https://github.com/mallowigi/a-file-icon-idea
    val hasImplementations = Emoji.fromFormatted("<:HasImplementations:1092550851303837736>")
    val hasOverrides = Emoji.fromFormatted("<:HasOverrides:1092550849269604472>")
    val hasSuperclasses = Emoji.fromFormatted("<:HasSuper:1092553895382884352>")
}