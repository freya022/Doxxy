package com.freya02.bot.docs

import com.freya02.bot.utils.Utils.isBCGuild
import com.freya02.botcommands.api.BContext
import com.freya02.botcommands.api.parameters.ParameterResolver
import com.freya02.botcommands.api.parameters.RegexParameterResolver
import com.freya02.botcommands.api.parameters.SlashParameterResolver
import com.freya02.botcommands.internal.commands.application.slash.SlashCommandInfo
import com.freya02.botcommands.internal.commands.prefixed.TextCommandVariation
import com.freya02.docs.DocSourceType
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.interactions.commands.Command
import net.dv8tion.jda.api.interactions.commands.CommandInteractionPayload
import net.dv8tion.jda.api.interactions.commands.OptionMapping
import net.dv8tion.jda.api.interactions.commands.OptionType
import java.util.regex.Pattern

class DocSourceTypeResolver : ParameterResolver<DocSourceTypeResolver, DocSourceType>(DocSourceType::class), SlashParameterResolver<DocSourceTypeResolver, DocSourceType>, RegexParameterResolver<DocSourceTypeResolver, DocSourceType> {
    override val optionType: OptionType = OptionType.STRING

    override fun resolve(
        context: BContext,
        info: SlashCommandInfo,
        event: CommandInteractionPayload,
        optionMapping: OptionMapping
    ): DocSourceType = DocSourceType.valueOf(optionMapping.asString)

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

    override fun resolve(
        context: BContext,
        variation: TextCommandVariation,
        event: MessageReceivedEvent,
        args: Array<String?>
    ): DocSourceType? {
        val typeStr = args.first()?.lowercase() ?: return null
        return when {
            typeStr.contentEquals("java", true) -> DocSourceType.JAVA
            typeStr.contentEquals("jda", true) -> DocSourceType.JDA
            typeStr.contentEquals("botcommands", true) -> DocSourceType.BOT_COMMANDS
            typeStr.contentEquals("bc", true) -> DocSourceType.BOT_COMMANDS
            else -> null
        }
    }

    override val pattern: Pattern = Pattern.compile("(?i)(JDA|java|BotCommands|BC)(?-i)")

    override val testExample: String = "jda"
}