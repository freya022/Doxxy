package com.freya02.bot.resolvers

import com.freya02.bot.tag.TagCriteria
import com.freya02.botcommands.api.BContext
import com.freya02.botcommands.api.parameters.ParameterResolver
import com.freya02.botcommands.api.parameters.SlashParameterResolver
import com.freya02.botcommands.internal.commands.application.slash.SlashCommandInfo
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.interactions.commands.Command
import net.dv8tion.jda.api.interactions.commands.CommandInteractionPayload
import net.dv8tion.jda.api.interactions.commands.OptionMapping
import net.dv8tion.jda.api.interactions.commands.OptionType

class TagCriteriaResolver : ParameterResolver<TagCriteriaResolver, TagCriteria>(TagCriteria::class), SlashParameterResolver<TagCriteriaResolver, TagCriteria> {
    override val optionType: OptionType = OptionType.STRING

    override fun resolve(
        context: BContext,
        info: SlashCommandInfo,
        event: CommandInteractionPayload,
        optionMapping: OptionMapping
    ): TagCriteria = TagCriteria.valueOf(optionMapping.asString)

    override fun getPredefinedChoices(guild: Guild?): Collection<Command.Choice> = listOf(
        Command.Choice("Name", TagCriteria.NAME.name),
        Command.Choice("Uses", TagCriteria.USES.name)
    )
}