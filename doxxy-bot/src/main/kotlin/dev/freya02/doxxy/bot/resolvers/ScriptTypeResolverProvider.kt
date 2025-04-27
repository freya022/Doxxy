package dev.freya02.doxxy.bot.resolvers

import dev.freya02.doxxy.bot.versioning.ScriptType
import io.github.freya022.botcommands.api.core.service.annotations.BConfiguration
import io.github.freya022.botcommands.api.core.service.annotations.Resolver
import io.github.freya022.botcommands.api.parameters.enumResolver

@BConfiguration
object ScriptTypeResolverProvider {
    @Resolver
    fun scriptTypeResolver() = enumResolver<ScriptType>()
}