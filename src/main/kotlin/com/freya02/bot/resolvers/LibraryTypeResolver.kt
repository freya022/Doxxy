package com.freya02.bot.resolvers

import com.freya02.bot.utils.Utils.isBCGuild
import com.freya02.bot.versioning.LibraryType
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
class LibraryTypeResolver : ClassParameterResolver<LibraryTypeResolver, LibraryType>(LibraryType::class),
    SlashParameterResolver<LibraryTypeResolver, LibraryType> {
    override val optionType: OptionType = OptionType.STRING

    override fun getPredefinedChoices(guild: Guild?): Collection<Command.Choice> {
        return when {
            guild.isBCGuild() -> listOf(
                Command.Choice("BotCommands", LibraryType.BOT_COMMANDS.name),
                Command.Choice("JDA", LibraryType.JDA.name),
                Command.Choice("JDA-KTX", LibraryType.JDA_KTX.name)
            )
            else -> listOf(
                Command.Choice("JDA", LibraryType.JDA.name),
                Command.Choice("JDA-KTX", LibraryType.JDA_KTX.name),
                Command.Choice("LavaPlayer", LibraryType.LAVA_PLAYER.name)
            )
        }
    }

    override fun resolve(
        info: SlashCommandInfo,
        event: CommandInteractionPayload,
        optionMapping: OptionMapping
    ): LibraryType = LibraryType.valueOf(optionMapping.asString)
}
