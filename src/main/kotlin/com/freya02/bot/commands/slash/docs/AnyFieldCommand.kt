package com.freya02.bot.commands.slash.docs

import com.freya02.bot.commands.slash.docs.CommonDocsHandlers.Companion.handleFieldDocs
import com.freya02.bot.docs.DocIndexMap
import com.freya02.botcommands.api.annotations.CommandMarker
import com.freya02.botcommands.api.commands.annotations.GeneratedOption
import com.freya02.botcommands.api.commands.application.annotations.AppOption
import com.freya02.botcommands.api.commands.application.slash.GuildSlashEvent
import com.freya02.botcommands.api.commands.application.slash.annotations.JDASlashCommand
import com.freya02.botcommands.api.components.Components
import com.freya02.docs.DocSourceType
import dev.minn.jda.ktx.messages.reply_

@CommandMarker
class AnyFieldCommand(private val docIndexMap: DocIndexMap, private val components: Components) : BaseDocCommand() {
    @JDASlashCommand(
        name = "anyfield",
        subcommand = "botcommands",
        description = "Shows the documentation for any field"
    )
    fun onSlashAnyFieldBC(
        event: GuildSlashEvent,
        @GeneratedOption sourceType: DocSourceType,
        @AppOption(
            description = "Full signature of the class + field",
            autocomplete = CommonDocsHandlers.ANY_FIELD_NAME_AUTOCOMPLETE_NAME
        ) fullSignature: String
    ) {
        onSlashAnyField(event, sourceType, fullSignature)
    }

    @JDASlashCommand(name = "anyfield", subcommand = "jda", description = "Shows the documentation for any field")
    fun onSlashAnyFieldJDA(
        event: GuildSlashEvent,
        @GeneratedOption sourceType: DocSourceType,
        @AppOption(
            description = "Full signature of the class + field",
            autocomplete = CommonDocsHandlers.ANY_FIELD_NAME_AUTOCOMPLETE_NAME
        ) fullSignature: String
    ) {
        onSlashAnyField(event, sourceType, fullSignature)
    }

    @JDASlashCommand(name = "anyfield", subcommand = "java", description = "Shows the documentation for any field")
    fun onSlashAnyFieldJava(
        event: GuildSlashEvent,
        @GeneratedOption sourceType: DocSourceType,
        @AppOption(
            description = "Full signature of the class + field",
            autocomplete = CommonDocsHandlers.ANY_FIELD_NAME_AUTOCOMPLETE_NAME
        ) fullSignature: String
    ) {
        onSlashAnyField(event, sourceType, fullSignature)
    }

    private fun onSlashAnyField(
        event: GuildSlashEvent,
        sourceType: DocSourceType,
        fullSignature: String
    ) {
        val split = fullSignature.split("#")
        if (split.size != 2) {
            event.reply_(
                "You must supply a class name and a field name, separated with a #, e.g. `Integer#MAX_VALUE`",
                ephemeral = true
            ).queue()
            return
        }

        val docIndex = docIndexMap[sourceType]!!
        handleFieldDocs(event, split[0], split[1], docIndex, components)
    }
}