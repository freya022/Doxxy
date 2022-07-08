package com.freya02.bot.commands.slash.docs

import com.freya02.botcommands.api.annotations.CommandMarker
import com.freya02.botcommands.api.application.annotations.AppOption
import com.freya02.botcommands.api.application.slash.GuildSlashEvent
import com.freya02.botcommands.api.application.slash.annotations.JDASlashCommand
import com.freya02.docs.DocSourceType
import dev.minn.jda.ktx.messages.reply_

@CommandMarker
class AnyMethodCommand : BaseDocCommand() {
    @JDASlashCommand(
        name = "anymethod",
        subcommand = "botcommands",
        description = "Shows the documentation for any method"
    )
    fun onSlashAnyMethodBC(
        event: GuildSlashEvent,
        @AppOption(description = "The docs to search upon") sourceType: DocSourceType,
        @AppOption(
            description = "Full signature of the class + method",
            autocomplete = CommonDocsHandlers.ANY_METHOD_NAME_AUTOCOMPLETE_NAME
        ) fullSignature: String
    ) {
        onSlashAnyMethod(event, sourceType, fullSignature)
    }

    @JDASlashCommand(name = "anymethod", subcommand = "jda", description = "Shows the documentation for any method")
    fun onSlashAnyMethodJDA(
        event: GuildSlashEvent,
        @AppOption(description = "The docs to search upon") sourceType: DocSourceType,
        @AppOption(
            description = "Full signature of the class + method",
            autocomplete = CommonDocsHandlers.ANY_METHOD_NAME_AUTOCOMPLETE_NAME
        ) fullSignature: String
    ) {
        onSlashAnyMethod(event, sourceType, fullSignature)
    }

    @JDASlashCommand(name = "anymethod", subcommand = "java", description = "Shows the documentation for any method")
    fun onSlashAnyMethodJava(
        event: GuildSlashEvent,
        @AppOption(description = "The docs to search upon") sourceType: DocSourceType,
        @AppOption(
            description = "Full signature of the class + method",
            autocomplete = CommonDocsHandlers.ANY_METHOD_NAME_AUTOCOMPLETE_NAME
        ) fullSignature: String
    ) {
        onSlashAnyMethod(event, sourceType, fullSignature)
    }

    private fun onSlashAnyMethod(
        event: GuildSlashEvent,
        sourceType: DocSourceType,
        fullSignature: String
    ) {
        val split = fullSignature.split("#".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        if (split.size != 2) {
            event.reply_(
                "You must supply a class name and a method signature, separated with a #, e.g. `Object#getClass()`",
                ephemeral = true
            ).queue()
            return
        }

        val docIndex = docIndexMap[sourceType]
        CommonDocsHandlers.handleMethodDocs(event, split[0], split[1], docIndex)
    }
}