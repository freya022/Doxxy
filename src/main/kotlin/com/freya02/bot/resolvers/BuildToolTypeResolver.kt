package com.freya02.bot.resolvers

import com.freya02.bot.versioning.supplier.BuildToolType
import com.freya02.botcommands.api.BContext
import com.freya02.botcommands.api.core.service.annotations.Resolver
import com.freya02.botcommands.api.parameters.ParameterResolver
import com.freya02.botcommands.api.parameters.SlashParameterResolver
import com.freya02.botcommands.internal.commands.application.slash.SlashCommandInfo
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.interactions.commands.Command
import net.dv8tion.jda.api.interactions.commands.CommandInteractionPayload
import net.dv8tion.jda.api.interactions.commands.OptionMapping
import net.dv8tion.jda.api.interactions.commands.OptionType

@Resolver
class BuildToolTypeResolver : ParameterResolver<BuildToolTypeResolver, BuildToolType>(BuildToolType::class),
    SlashParameterResolver<BuildToolTypeResolver, BuildToolType> {
    override val optionType: OptionType = OptionType.STRING

    override fun resolve(
        context: BContext,
        info: SlashCommandInfo,
        event: CommandInteractionPayload,
        optionMapping: OptionMapping
    ): BuildToolType = BuildToolType.valueOf(optionMapping.asString)

    override fun getPredefinedChoices(guild: Guild?): Collection<Command.Choice> {
        return BuildToolType.entries.map { Command.Choice(it.humanName, it.name) }
    }
}
