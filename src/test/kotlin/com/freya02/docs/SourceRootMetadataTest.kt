package com.freya02.docs

import com.freya02.bot.Data
import com.freya02.bot.docs.metadata.parser.SourceRootMetadata

fun main() {
    val metadata = SourceRootMetadata(Data.javadocsPath.resolve("JDA"))

    println(
        metadata.implementationMetadata
            .getClassBySimpleName("Message")
            .getDeclaredMethodsByName("getContentRaw")
            .values.map { it.implementations }
    )

    println(
        metadata.implementationMetadata
            .getClassBySimpleName("IPermissionContainerManager")
            .getDeclaredMethodsByName("removePermissionOverride")
            .values.map { it.implementations }
    )

    println(
        metadata.implementationMetadata
            .getClassBySimpleName("VoiceChannelManager")
            .getMethodsByName("removePermissionOverride")
            .map { it.implementations }
    )

    println()
}