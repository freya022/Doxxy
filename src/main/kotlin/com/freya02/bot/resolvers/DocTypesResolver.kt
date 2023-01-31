package com.freya02.bot.resolvers

import com.freya02.bot.docs.index.DocTypes
import com.freya02.botcommands.api.BContext
import com.freya02.botcommands.api.parameters.ParameterResolver
import com.freya02.botcommands.api.parameters.SlashParameterResolver
import com.freya02.botcommands.internal.commands.application.slash.SlashCommandInfo
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.interactions.commands.Command.Choice
import net.dv8tion.jda.api.interactions.commands.CommandInteractionPayload
import net.dv8tion.jda.api.interactions.commands.OptionMapping
import net.dv8tion.jda.api.interactions.commands.OptionType

class DocTypesResolver : ParameterResolver<DocTypesResolver, DocTypes>(DocTypes::class),
    SlashParameterResolver<DocTypesResolver, DocTypes> {
    override val optionType: OptionType = OptionType.INTEGER

    override fun getPredefinedChoices(guild: Guild?): Collection<Choice> {
        return listOf(
            Choice("Class", DocTypes.CLASS.getRaw()),
            Choice("Method", DocTypes.METHOD.getRaw()),
            Choice("Field", DocTypes.FIELD.getRaw()),
            Choice("Any", DocTypes.ANY.getRaw())
        )
    }

    override suspend fun resolveSuspend(context: BContext, info: SlashCommandInfo, event: CommandInteractionPayload, optionMapping: OptionMapping): DocTypes? {
        return DocTypes.fromRaw(optionMapping.asLong)
    }
}
