package dev.freya02.doxxy.bot.utils

object ParsingUtils {
    val spaceRegex = Regex("""\s+""")
    val codeBlockRegex = Regex("""```.*\n(\X*?)```""")
}