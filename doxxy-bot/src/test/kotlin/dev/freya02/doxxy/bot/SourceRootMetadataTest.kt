package dev.freya02.doxxy.bot

import dev.freya02.doxxy.bot.docs.DocSourceType
import dev.freya02.doxxy.bot.docs.javadocDirectory
import dev.freya02.doxxy.bot.docs.metadata.parser.SourceMetadata
import io.github.freya022.botcommands.api.emojis.AppEmojisRegistry
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic

fun main() {
    // [[ClassType]] gets instantiated so we need to mock app emojis
    mockkStatic(AppEmojisRegistry::class)
    every { AppEmojisRegistry[any()] } returns mockk()

    val metadata = SourceMetadata(DocSourceType.JDA.javadocDirectory)

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