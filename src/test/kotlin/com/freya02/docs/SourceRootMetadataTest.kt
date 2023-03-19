package com.freya02.docs

import com.freya02.bot.Data
import com.freya02.bot.docs.metadata.SourceRootMetadata

fun main() {
    val metadata = SourceRootMetadata(Data.javadocsPath.resolve("JDA"))

    println(
        metadata.implementationMetadata
            .getClassBySimpleName("Message")
            .getMethodsByName("getContentRaw")
            .values.map { it.implementations }
    )

    println(
        metadata.implementationMetadata
            .getClassBySimpleName("IPermissionContainerManager")
            .getMethodsByName("removePermissionOverride")
            .values.map { it.implementations }
    )

    println()
}