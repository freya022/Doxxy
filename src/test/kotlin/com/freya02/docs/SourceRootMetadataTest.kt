package com.freya02.docs

import com.freya02.bot.Main
import com.freya02.bot.docs.metadata.SourceRootMetadata
import com.freya02.bot.utils.Utils.measureTime

fun main() {
    SourceRootMetadata(Main.JAVADOCS_PATH.resolve("JDA"))

    val metadata = measureTime("Metadata") {
        SourceRootMetadata(Main.JAVADOCS_PATH.resolve("JDA"))
    }

    println()
}