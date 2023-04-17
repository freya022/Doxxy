package com.freya02.docs

import com.freya02.bot.format.Formatter

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