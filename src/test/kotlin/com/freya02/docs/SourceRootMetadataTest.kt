package com.freya02.docs

import com.freya02.bot.Data
import com.freya02.bot.docs.metadata.SourceRootMetadata

fun main() {
    //TODO IPermissionContainerManager is not in method implementations
    //TODO Remove duplicates from subclasses & possibly method implementations
    val metadata = SourceRootMetadata(Data.javadocsPath.resolve("JDA"))

    println()
}