package com.freya02.docs

import com.freya02.bot.Data
import com.freya02.bot.docs.metadata.ImplementationMetadata.Companion.findByMethodName
import com.freya02.bot.docs.metadata.ImplementationMetadata.Companion.findDeclByClassName
import com.freya02.bot.docs.metadata.ImplementationMetadata.Companion.flattenReferences
import com.freya02.bot.docs.metadata.SourceRootMetadata

fun main() {
    val metadata = SourceRootMetadata(Data.javadocsPath.resolve("JDA"))

    println(
        metadata.implementationMetadata.classToMethodImplementations
            .findDeclByClassName("Message")
            .findByMethodName("getContentRaw")
            .flattenReferences()
    )

    println(
        metadata.implementationMetadata.classToMethodImplementations
            .findDeclByClassName("IPermissionContainerManager")
            .findByMethodName("removePermissionOverride")
            .flattenReferences()
    )

    println()
}