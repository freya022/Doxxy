package dev.freya02.doxxy.bot.resolvers

import dev.freya02.doxxy.bot.docs.index.DocTypes
import io.github.freya022.botcommands.api.commands.application.slash.options.SlashCommandOption
import io.github.freya022.botcommands.api.core.service.annotations.Resolver
import io.github.freya022.botcommands.api.parameters.ClassParameterResolver
import io.github.freya022.botcommands.api.parameters.resolvers.SlashParameterResolver
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.interactions.commands.Command.Choice
import net.dv8tion.jda.api.interactions.commands.CommandInteractionPayload
import net.dv8tion.jda.api.interactions.commands.OptionMapping
import net.dv8tion.jda.api.interactions.commands.OptionType

@Resolver
class DocTypesResolver : ClassParameterResolver<DocTypesResolver, DocTypes>(DocTypes::class),
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

    override suspend fun resolveSuspend(option: SlashCommandOption, event: CommandInteractionPayload, optionMapping: OptionMapping): DocTypes {
        return DocTypes.fromRaw(optionMapping.asLong)
    }
}
