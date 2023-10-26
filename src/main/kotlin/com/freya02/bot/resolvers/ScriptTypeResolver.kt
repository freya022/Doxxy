package com.freya02.bot.resolvers

import com.freya02.bot.versioning.ScriptType
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
class ScriptTypeResolver : ClassParameterResolver<ScriptTypeResolver, ScriptType>(ScriptType::class),
    SlashParameterResolver<ScriptTypeResolver, ScriptType> {
    override val optionType: OptionType = OptionType.STRING

    override fun getPredefinedChoices(guild: Guild?): Collection<Command.Choice> {
        return ScriptType.entries
            .map { type ->
                Command.Choice(type.name.lowercase().replaceFirstChar { c -> c.uppercaseChar() }, type.name)
            }
    }

    override fun resolve(
        info: SlashCommandInfo,
        event: CommandInteractionPayload,
        optionMapping: OptionMapping
    ): ScriptType = ScriptType.valueOf(optionMapping.asString)
}