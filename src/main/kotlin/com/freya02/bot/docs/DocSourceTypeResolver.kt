package com.freya02.bot.docs

import com.freya02.bot.utils.Utils.isBCGuild
import com.freya02.botcommands.api.BContext
import com.freya02.botcommands.api.parameters.ParameterResolver
import com.freya02.botcommands.api.parameters.SlashParameterResolver
import com.freya02.botcommands.internal.application.slash.SlashCommandInfo
import com.freya02.docs.DocSourceType
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.interactions.commands.Command
import net.dv8tion.jda.api.interactions.commands.CommandInteractionPayload
import net.dv8tion.jda.api.interactions.commands.OptionMapping
import net.dv8tion.jda.api.interactions.commands.OptionType

class DocSourceTypeResolver : ParameterResolver(DocSourceType::class.java), SlashParameterResolver {
    override fun getOptionType(): OptionType = OptionType.STRING

    override fun resolve(
        context: BContext,
        info: SlashCommandInfo,
        event: CommandInteractionPayload,
        optionMapping: OptionMapping
    ): Any = DocSourceType.valueOf(optionMapping.asString)

    override fun getPredefinedChoices(guild: Guild?): Collection<Command.Choice> {
        return when {
            guild.isBCGuild() -> listOf(
                Command.Choice("BotCommands", DocSourceType.BOT_COMMANDS.name),
                Command.Choice("JDA", DocSourceType.JDA.name),
                Command.Choice("Java", DocSourceType.JAVA.name)
            )
            else -> listOf(
                Command.Choice("JDA", DocSourceType.JDA.name),
                Command.Choice("Java", DocSourceType.JAVA.name)
            )
        }
    }
}