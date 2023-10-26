package com.freya02.bot.resolvers

import com.freya02.bot.utils.Utils.isBCGuild
import com.freya02.docs.DocSourceType
import io.github.freya022.botcommands.api.commands.text.BaseCommandEvent
import io.github.freya022.botcommands.api.core.service.annotations.Resolver
import io.github.freya022.botcommands.api.parameters.ClassParameterResolver
import io.github.freya022.botcommands.api.parameters.resolvers.ComponentParameterResolver
import io.github.freya022.botcommands.api.parameters.resolvers.SlashParameterResolver
import io.github.freya022.botcommands.api.parameters.resolvers.TextParameterResolver
import io.github.freya022.botcommands.internal.commands.application.slash.SlashCommandInfo
import io.github.freya022.botcommands.internal.commands.prefixed.TextCommandVariation
import io.github.freya022.botcommands.internal.components.ComponentDescriptor
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.interactions.commands.Command
import net.dv8tion.jda.api.interactions.commands.CommandInteractionPayload
import net.dv8tion.jda.api.interactions.commands.OptionMapping
import net.dv8tion.jda.api.interactions.commands.OptionType
import java.util.regex.Pattern
import kotlin.reflect.KParameter

@Resolver
class DocSourceTypeResolver : ClassParameterResolver<DocSourceTypeResolver, DocSourceType>(DocSourceType::class),
    SlashParameterResolver<DocSourceTypeResolver, DocSourceType>,
    TextParameterResolver<DocSourceTypeResolver, DocSourceType>,
    ComponentParameterResolver<DocSourceTypeResolver, DocSourceType> {
    override val optionType: OptionType = OptionType.STRING

    override fun resolve(
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

    override fun getHelpExample(parameter: KParameter, event: BaseCommandEvent, isID: Boolean): String {
        return "jda"
    }

    override fun resolve(
        descriptor: ComponentDescriptor,
        event: GenericComponentInteractionCreateEvent,
        arg: String
    ): DocSourceType? {
        return DocSourceType.fromIdOrNull(arg.toInt())
    }
}