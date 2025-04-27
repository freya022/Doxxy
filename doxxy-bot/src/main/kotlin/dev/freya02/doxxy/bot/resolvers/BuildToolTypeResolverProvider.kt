package dev.freya02.doxxy.bot.resolvers

import dev.freya02.doxxy.bot.versioning.supplier.BuildToolType
import io.github.freya022.botcommands.api.core.service.annotations.BConfiguration
import io.github.freya022.botcommands.api.core.service.annotations.Resolver
import io.github.freya022.botcommands.api.parameters.enumResolver

@BConfiguration
object BuildToolTypeResolverProvider {
    @Resolver
    fun buildToolTypeResolver() = enumResolver(nameFunction = BuildToolType::humanName)
}
