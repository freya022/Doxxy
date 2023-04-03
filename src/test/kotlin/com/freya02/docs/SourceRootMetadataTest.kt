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

    //TODO add logic to find implementations from superclasses, looking at superclasses and their method implementations should be enough,
    // but their declaring type need to be a superclass or subclass of the target class
    //   example: VoiceChannelManager#removePermissionOverride, should look at superclasses,
    //   find IPermissionContainerManager which has that said method, and then see that ChannelManagerImpl... isn't in the class tree
    //   Maybe methods that are found to implement something but does not have the Override annotation should be marked as indirect implementations
    //   In which case they are always included in the implementations

    println()
}