package com.freya02.bot.resolvers

import com.freya02.bot.logback.LogbackProfile
import io.github.freya022.botcommands.api.core.service.annotations.Resolver
import io.github.freya022.botcommands.api.parameters.enumResolver

@Suppress("unused")
object LogbackProfileResolver {
    @Resolver
    fun get() = enumResolver<LogbackProfile> { p ->
        p.name.lowercase()
            .replaceFirstChar { it.uppercaseChar() }
            .replace('_', ' ')
    }
}
