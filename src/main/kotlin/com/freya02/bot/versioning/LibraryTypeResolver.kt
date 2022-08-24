package com.freya02.bot.versioning

import com.freya02.bot.utils.Utils.isBCGuild
import com.freya02.botcommands.api.BContext
import com.freya02.botcommands.api.parameters.ParameterResolver
import com.freya02.botcommands.api.parameters.SlashParameterResolver
import com.freya02.botcommands.internal.commands.application.slash.SlashCommandInfo
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.interactions.commands.Command
import net.dv8tion.jda.api.interactions.commands.CommandInteractionPayload
import net.dv8tion.jda.api.interactions.commands.OptionMapping
import net.dv8tion.jda.api.interactions.commands.OptionType

class LibraryTypeResolver : ParameterResolver<LibraryTypeResolver, LibraryType>(LibraryType::class), SlashParameterResolver<LibraryTypeResolver, LibraryType> {
    override val optionType: OptionType = OptionType.STRING

    override fun getPredefinedChoices(guild: Guild?): Collection<Command.Choice> {
        return when {
            guild.isBCGuild() -> listOf(
                Command.Choice("BotCommands", LibraryType.BOT_COMMANDS.name),
                Command.Choice("JDA 5", LibraryType.JDA5.name),
                Command.Choice("JDA 4", LibraryType.JDA4.name)
            )
            else -> listOf(
                Command.Choice("JDA 5", LibraryType.JDA5.name),
                Command.Choice("JDA 4", LibraryType.JDA4.name)
            )
        }
    }

    override fun resolve(
        context: BContext,
        info: SlashCommandInfo,
        event: CommandInteractionPayload,
        optionMapping: OptionMapping
    ): LibraryType = LibraryType.valueOf(optionMapping.asString)
}