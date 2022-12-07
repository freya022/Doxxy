package com.freya02.bot.utils

import ch.qos.logback.classic.Level
import com.freya02.botcommands.api.BContext
import com.freya02.botcommands.api.parameters.ParameterResolver
import com.freya02.botcommands.api.parameters.SlashParameterResolver
import com.freya02.botcommands.internal.application.slash.SlashCommandInfo
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.interactions.commands.Command.Choice
import net.dv8tion.jda.api.interactions.commands.CommandInteractionPayload
import net.dv8tion.jda.api.interactions.commands.OptionMapping
import net.dv8tion.jda.api.interactions.commands.OptionType

class LogLevelResolver : ParameterResolver(Level::class.java), SlashParameterResolver {
    override fun getOptionType() = OptionType.STRING

    override fun getPredefinedChoices(guild: Guild?): Collection<Choice> =
        listOf(Level.TRACE, Level.DEBUG, Level.INFO, Level.WARN, Level.ERROR, Level.OFF, Level.ALL)
            .map { it.levelStr }
            .map { Choice(it, it) }

    override fun resolve(
        context: BContext,
        info: SlashCommandInfo,
        event: CommandInteractionPayload,
        optionMapping: OptionMapping
    ): Level = Level.valueOf(optionMapping.asString)
}