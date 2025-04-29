package dev.freya02.doxxy.docs

import dev.freya02.doxxy.bot.format.Formatter

fun main() {
    println(
        Formatter.format(
            """
            String bruh = "lmao"; 
            
            String sike;
            
            
            """.trimIndent()
        )
    )
}