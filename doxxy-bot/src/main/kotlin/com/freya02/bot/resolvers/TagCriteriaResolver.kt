package com.freya02.bot.resolvers

import com.freya02.bot.tag.TagCriteria
import io.github.freya022.botcommands.api.core.service.annotations.Resolver
import io.github.freya022.botcommands.api.parameters.ClassParameterResolver
import io.github.freya022.botcommands.api.parameters.resolvers.SlashParameterResolver
import io.github.freya022.botcommands.internal.commands.application.slash.SlashCommandInfo
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.interactions.commands.Command
import net.dv8tion.jda.api.interactions.commands.CommandInteractionPayload
import net.dv8tion.jda.api.interactions.commands.OptionMapping
import net.dv8tion.jda.api.interactions.commands.OptionType

@Resolver
class TagCriteriaResolver : ClassParameterResolver<TagCriteriaResolver, TagCriteria>(TagCriteria::class),
    SlashParameterResolver<TagCriteriaResolver, TagCriteria> {
    override val optionType: OptionType = OptionType.STRING

    override fun resolve(
        info: SlashCommandInfo,
        event: CommandInteractionPayload,
        optionMapping: OptionMapping
    ): TagCriteria = TagCriteria.valueOf(optionMapping.asString)

    override fun getPredefinedChoices(guild: Guild?): Collection<Command.Choice> = listOf(
        Command.Choice("Name", TagCriteria.NAME.name),
        Command.Choice("Uses", TagCriteria.USES.name)
    )
}