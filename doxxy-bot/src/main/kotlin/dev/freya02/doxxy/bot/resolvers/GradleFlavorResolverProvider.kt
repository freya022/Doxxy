package dev.freya02.doxxy.bot.resolvers

import dev.freya02.doxxy.bot.versioning.supplier.GradleFlavor
import io.github.freya022.botcommands.api.core.service.annotations.BConfiguration
import io.github.freya022.botcommands.api.core.service.annotations.Resolver
import io.github.freya022.botcommands.api.parameters.enumResolver
import io.github.freya022.botcommands.api.parameters.toHumanName

@BConfiguration
object GradleFlavorResolverProvider {
    @Resolver
    fun gradleFlavorResolver() = enumResolver<GradleFlavor>(nameFunction = { "${it.toHumanName()} (${it.scriptName})" })
}
