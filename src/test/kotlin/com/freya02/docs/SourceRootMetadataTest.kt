package com.freya02.docs

import com.freya02.bot.Data
import com.freya02.bot.docs.metadata.SourceRootMetadata
import com.freya02.bot.utils.Utils.measureTime

fun main() {
    SourceRootMetadata(Data.javadocsPath.resolve("JDA"))

    val metadata = measureTime("Metadata") {
        SourceRootMetadata(Data.javadocsPath.resolve("JDA"))
    }

    println()
}