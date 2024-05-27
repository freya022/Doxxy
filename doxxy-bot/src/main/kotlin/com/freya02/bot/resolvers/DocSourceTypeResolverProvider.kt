package com.freya02.bot.resolvers

import com.freya02.docs.DocSourceType
import io.github.freya022.botcommands.api.commands.text.BaseCommandEvent
import io.github.freya022.botcommands.api.core.service.annotations.BConfiguration
import io.github.freya022.botcommands.api.core.service.annotations.Resolver
import io.github.freya022.botcommands.api.parameters.ClassParameterResolver
import io.github.freya022.botcommands.api.parameters.enumResolver
import io.github.freya022.botcommands.api.parameters.resolvers.TextParameterResolver
import io.github.freya022.botcommands.internal.commands.text.TextCommandVariation
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import java.util.regex.Pattern
import kotlin.reflect.KParameter

@BConfiguration
object DocSourceTypeResolverProvider {
    @Resolver
    fun docSourceTypeResolver() = enumResolver<DocSourceType>()

    object TextDocSourceTypeResolver :
        ClassParameterResolver<TextDocSourceTypeResolver, DocSourceType>(DocSourceType::class),
        TextParameterResolver<TextDocSourceTypeResolver, DocSourceType> {

        override fun resolve(
            variation: TextCommandVariation,
            event: MessageReceivedEvent,
            args: Array<String?>
        ): DocSourceType? {
            val typeStr = args.first()?.lowercase() ?: return null
            return when {
                typeStr.contentEquals("java", true) -> DocSourceType.JAVA
                typeStr.contentEquals("jda", true) -> DocSourceType.JDA
                else -> null
            }
        }

        override val pattern: Pattern = Pattern.compile("(?i)(JDA|java)(?-i)")

        override val testExample: String = "jda"

        override fun getHelpExample(parameter: KParameter, event: BaseCommandEvent, isID: Boolean): String {
            return "jda"
        }
    }

    @Resolver
    fun textDocSourceTypeResolver() = TextDocSourceTypeResolver
}