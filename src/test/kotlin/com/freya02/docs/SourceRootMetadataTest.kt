package com.freya02.docs

import com.freya02.bot.Data
import com.freya02.bot.docs.metadata.SourceRootMetadata

fun main() {
    //TODO IPermissionContainerManager is not in method implementations
    //FIXED UpdateEvent#getOldValue is not in #allMethods as it is being shadowed by implementations using the same return type & signature
    //TODO UpdateEvent#getOldValue is reported to be implemented in every subclass
    //TODO Remove duplicates from subclasses & possibly method implementations
    val metadata = SourceRootMetadata(Data.javadocsPath.resolve("JDA"))

    println()
}