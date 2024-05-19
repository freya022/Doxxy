package com.freya02.bot.resolvers

import com.freya02.bot.logback.LogbackProfile
import io.github.freya022.botcommands.api.core.service.annotations.BConfiguration
import io.github.freya022.botcommands.api.core.service.annotations.Resolver
import io.github.freya022.botcommands.api.parameters.enumResolver

@BConfiguration
object LogbackProfileResolverProvider {
    @Resolver
    fun logbackProfileResolver() = enumResolver<LogbackProfile>()
}
