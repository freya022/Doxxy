package dev.freya02.doxxy.bot.resolvers

import dev.freya02.doxxy.bot.tag.TagCriteria
import io.github.freya022.botcommands.api.core.service.annotations.BConfiguration
import io.github.freya022.botcommands.api.core.service.annotations.Resolver
import io.github.freya022.botcommands.api.parameters.enumResolver

@BConfiguration
object TagCriteriaResolverProvider {
    @Resolver
    fun tagCriteriaResolver() = enumResolver<TagCriteria>()
}