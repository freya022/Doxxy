package com.freya02.bot.utils

object ParsingUtils {
    val spaceRegex = Regex("""\s+""")
    val codeBlockRegex = Regex("""```.*\n(\X*?)```""")
}