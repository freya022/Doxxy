package com.freya02.bot.resolvers

import com.freya02.bot.utils.Utils.isBCGuild
import com.freya02.bot.versioning.LibraryType
import com.freya02.bot.versioning.LibraryType.*
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
