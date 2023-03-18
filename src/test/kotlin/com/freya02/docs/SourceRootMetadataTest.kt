package com.freya02.docs

import com.freya02.bot.Data
import com.freya02.bot.docs.metadata.SourceRootMetadata
import com.freya02.bot.docs.metadata.findByMethodName
import com.freya02.bot.docs.metadata.findDeclByClassName
import com.freya02.bot.docs.metadata.flattenReferences

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