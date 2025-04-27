package dev.freya02.doxxy.bot.resolvers

import dev.freya02.doxxy.bot.utils.Utils.isBCGuild
import dev.freya02.doxxy.bot.versioning.LibraryType
import dev.freya02.doxxy.bot.versioning.LibraryType.*
import io.github.freya022.botcommands.api.core.service.annotations.BConfiguration
import io.github.freya022.botcommands.api.core.service.annotations.Resolver
import io.github.freya022.botcommands.api.core.utils.enumSetOf
import io.github.freya022.botcommands.api.parameters.enumResolver

@BConfiguration
object LibraryTypeResolverProvider {
    @Resolver
    fun libraryTypeResolver() = enumResolver(
        guildValuesSupplier = { guild ->
            if (guild.isBCGuild()) {
                enumSetOf(BOT_COMMANDS, JDA, JDA_KTX)
            } else {
                enumSetOf(JDA, JDA_KTX, LAVA_PLAYER)
            }
        },
        nameFunction = LibraryType::displayString
    )
}
