package com.freya02.bot.docs.index

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
import java.util.*

class DocTypes(private val set: Set<DocType>): Set<DocType> by set {
    constructor(vararg types: DocType) : this(types.toSet())

    fun getRaw(): Long = this.fold(0) { acc, type -> acc or type.raw }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DocTypes

        if (set != other.set) return false

        return true
    }

    override fun hashCode(): Int {
        return set.hashCode()
    }

    companion object {
        val CLASS = DocTypes(DocType.CLASS)
        val METHOD = DocTypes(DocType.METHOD)
        val FIELD = DocTypes(DocType.FIELD)
        val ANY = DocTypes(DocType.CLASS, DocType.METHOD, DocType.FIELD)

        val IDENTIFIERS = DocTypes(DocType.FIELD, DocType.METHOD)

        fun fromRaw(raw: Long): DocTypes {
            val set = EnumSet.noneOf(DocType::class.java)
            for (type in DocType.values()) {
                if ((raw and type.raw) == type.raw) {
                    set.add(type)
                }
            }

            return DocTypes(set)
        }
    }
}

@Resolver
class DocTypesResolver : ParameterResolver<DocTypesResolver, DocTypes>(DocTypes::class),
    SlashParameterResolver<DocTypesResolver, DocTypes> {
    override val optionType: OptionType = OptionType.INTEGER

    override fun getPredefinedChoices(guild: Guild?): Collection<Command.Choice> {
        return listOf(
            Command.Choice("Class", DocTypes.CLASS.getRaw()),
            Command.Choice("Method", DocTypes.METHOD.getRaw()),
            Command.Choice("Field", DocTypes.FIELD.getRaw()),
            Command.Choice("Any", DocTypes.ANY.getRaw())
        )
    }

    override suspend fun resolveSuspend(context: BContext, info: SlashCommandInfo, event: CommandInteractionPayload, optionMapping: OptionMapping): DocTypes? {
        return DocTypes.fromRaw(optionMapping.asLong)
    }
}
