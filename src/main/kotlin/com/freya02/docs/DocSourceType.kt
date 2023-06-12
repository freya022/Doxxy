package com.freya02.docs

import com.freya02.bot.utils.Utils.isBCGuild
import com.freya02.botcommands.api.BContext
import com.freya02.botcommands.api.core.service.annotations.Resolver
import com.freya02.botcommands.api.parameters.ComponentParameterResolver
import com.freya02.botcommands.api.parameters.ParameterResolver
import com.freya02.botcommands.api.parameters.RegexParameterResolver
import com.freya02.botcommands.api.parameters.SlashParameterResolver
import com.freya02.botcommands.internal.commands.application.slash.SlashCommandInfo
import com.freya02.botcommands.internal.commands.prefixed.TextCommandVariation
import com.freya02.botcommands.internal.components.ComponentDescriptor
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.interactions.commands.Command
import net.dv8tion.jda.api.interactions.commands.CommandInteractionPayload
import net.dv8tion.jda.api.interactions.commands.OptionMapping
import net.dv8tion.jda.api.interactions.commands.OptionType
import java.util.regex.Pattern

enum class DocSourceType(
    val id: Int,
    val cmdName: String,
    val sourceUrl: String,
    val sourceFolderName: String?,
    private val onlineURL: String?,
    vararg validPackagePatterns: String
) {
    JDA(
        1,
        "jda",
        "http://localhost:25566/JDA",
        "JDA",
        "https://docs.jda.wiki",
        "net\\.dv8tion\\.jda.*"
    ),
    BOT_COMMANDS(
        2,
        "botcommands",
        "http://localhost:25566/BotCommands",
        null,
        "https://freya022.github.io/BotCommands",
        "com\\.freya02\\.botcommands\\.api.*"
    ),
    JAVA(
        3,
        "java",
        "https://docs.oracle.com/en/java/javase/17/docs/api",
        null,
        "https://docs.oracle.com/en/java/javase/17/docs/api",
        "java\\.io.*",
        "java\\.lang",
        "java\\.lang\\.annotation.*",
        "java\\.lang\\.invoke.*",
        "java\\.lang\\.reflect.*",
        "java\\.math.*",
        "java\\.nio",
        "java\\.nio\\.channels",
        "java\\.nio\\.file",
        "java\\.sql.*",
        "java\\.time.*",
        "java\\.text.*",
        "java\\.security.*",
        "java\\.util",
        "java\\.util\\.concurrent.*",
        "java\\.util\\.function",
        "java\\.util\\.random",
        "java\\.util\\.regex",
        "java\\.util\\.stream"
    );

    private val validPackagePatterns: List<Regex> = validPackagePatterns.map { it.toRegex() }

    val allClassesIndexURL: String = "$sourceUrl/allclasses-index.html"
    val constantValuesURL: String = "$sourceUrl/constant-values.html"

    fun toEffectiveURL(url: String): String {
        if (onlineURL == null) return url

        return when {
            url.startsWith(sourceUrl) -> onlineURL + url.substring(sourceUrl.length)
            else -> url
        }
    }

    fun toOnlineURL(url: String): String? {
        if (onlineURL == null) return null

        return when {
            url.startsWith(sourceUrl) -> onlineURL + url.substring(sourceUrl.length)
            else -> url
        }
    }

    fun isValidPackage(packageName: String?): Boolean {
        if (packageName == null) return false

        return validPackagePatterns.any { packageName.matches(it) }
    }

    companion object {
        fun fromId(id: Int): DocSourceType {
            return values().find { it.id == id } ?: throw IllegalArgumentException("Unknown source ID $id")
        }

        fun fromIdOrNull(id: Int): DocSourceType? {
            return values().find { it.id == id }
        }

        fun fromUrl(url: String): DocSourceType? {
            return values().find { source -> url.startsWith(source.sourceUrl) || source.onlineURL != null && url.startsWith(source.onlineURL) }
        }

        fun typesForGuild(guild: Guild): List<DocSourceType> = when {
            guild.isBCGuild() -> values().asList()
            else -> values().asList() - BOT_COMMANDS
        }
    }
}

@Resolver
class DocSourceTypeResolver : ParameterResolver<DocSourceTypeResolver, DocSourceType>(DocSourceType::class),
    SlashParameterResolver<DocSourceTypeResolver, DocSourceType>,
    RegexParameterResolver<DocSourceTypeResolver, DocSourceType>,
    ComponentParameterResolver<DocSourceTypeResolver, DocSourceType> {
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

    override fun resolve(
        context: BContext,
        descriptor: ComponentDescriptor,
        event: GenericComponentInteractionCreateEvent,
        arg: String
    ): DocSourceType? {
        return DocSourceType.fromIdOrNull(arg.toInt())
    }
}