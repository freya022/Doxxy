package dev.freya02.doxxy.bot

object QueryRegexTest {
    private val queryRegex = Regex("""^(\w*)#?(\w*)""")

    @JvmStatic
    fun main(args: Array<String>) {
        val all = """
            lmao#ok
            lmao#
            #ok
            ok
            
        """.trimIndent()
            .lines()
            .map { queryRegex.matchEntire(it) }

        println()
    }
}
