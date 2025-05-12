package dev.freya02.doxxy.bot

import dev.freya02.doxxy.bot.docs.DocSourceType
import dev.freya02.doxxy.bot.docs.javadocDirectory
import dev.freya02.doxxy.bot.docs.metadata.parser.SourceRootMetadata

fun main() {
    val metadata = SourceRootMetadata(DocSourceType.JDA.javadocDirectory)

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