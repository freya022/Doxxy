package com.freya02.bot.versioning

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

enum class ScriptType(val humanName: String, val folderName: String) {
    DEPENDENCIES("Dependencies", "dependencies_scripts"),
    FULL("Full","build_scripts")
}

@Resolver
class ScriptTypeResolver : ParameterResolver<ScriptTypeResolver, ScriptType>(ScriptType::class),
    SlashParameterResolver<ScriptTypeResolver, ScriptType> {
    override val optionType: OptionType = OptionType.STRING

    override fun getPredefinedChoices(guild: Guild?): Collection<Command.Choice> {
        return ScriptType.values()
            .map { type -> Command.Choice(type.humanName, type.name) }
    }

    override fun resolve(
        context: BContext,
        info: SlashCommandInfo,
        event: CommandInteractionPayload,
        optionMapping: OptionMapping
    ): ScriptType = ScriptType.valueOf(optionMapping.asString)
}