package com.freya02.bot.commands.slash.docs

import com.freya02.botcommands.api.annotations.CommandMarker
import com.freya02.botcommands.api.application.annotations.AppOption
import com.freya02.botcommands.api.application.slash.GuildSlashEvent
import com.freya02.botcommands.api.application.slash.annotations.JDASlashCommand
import com.freya02.docs.DocSourceType

@CommandMarker
class ClassCommand : BaseDocCommand() {
    @JDASlashCommand(name = "class", subcommand = "botcommands", description = "Shows the documentation for a class")
    fun onSlashClassBC(
        event: GuildSlashEvent,
        @AppOption(description = "The docs to search upon") sourceType: DocSourceType,
        @AppOption(
            description = "Name of the Java class",
            autocomplete = CommonDocsHandlers.CLASS_NAME_AUTOCOMPLETE_NAME
        ) className: String
    ) {
        onSlashClass(event, sourceType, className)
    }

    @JDASlashCommand(name = "class", subcommand = "jda", description = "Shows the documentation for a class")
    fun onSlashClassJDA(
        event: GuildSlashEvent,
        @AppOption(description = "The docs to search upon") sourceType: DocSourceType,
        @AppOption(
            description = "Name of the Java class",
            autocomplete = CommonDocsHandlers.CLASS_NAME_AUTOCOMPLETE_NAME
        ) className: String
    ) {
        onSlashClass(event, sourceType, className)
    }

    @JDASlashCommand(name = "class", subcommand = "java", description = "Shows the documentation for a class")
    fun onSlashClassJava(
        event: GuildSlashEvent,
        @AppOption(description = "The docs to search upon") sourceType: DocSourceType,
        @AppOption(
            description = "Name of the Java class",
            autocomplete = CommonDocsHandlers.CLASS_NAME_AUTOCOMPLETE_NAME
        ) className: String
    ) {
        onSlashClass(event, sourceType, className)
    }

    private fun onSlashClass(
        event: GuildSlashEvent,
        sourceType: DocSourceType,
        className: String
    ) {
        val docIndex = docIndexMap[sourceType]!!
        CommonDocsHandlers.handleClass(event, className, docIndex)
    }
}