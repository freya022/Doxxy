package com.freya02.bot.resolvers

import com.freya02.docs.DocSourceType
import io.github.freya022.botcommands.api.core.service.annotations.BConfiguration
import io.github.freya022.botcommands.api.core.service.annotations.Resolver
import io.github.freya022.botcommands.api.parameters.enumResolver

@BConfiguration
object DocSourceTypeResolverProvider {
    @Resolver
    fun docSourceTypeResolver() = enumResolver<DocSourceType>()
}