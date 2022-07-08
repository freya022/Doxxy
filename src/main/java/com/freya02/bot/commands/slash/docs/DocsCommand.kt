package com.freya02.bot.commands.slash.docs

import com.freya02.botcommands.api.annotations.CommandMarker
import com.freya02.botcommands.api.application.annotations.AppOption
import com.freya02.botcommands.api.application.slash.GuildSlashEvent
import com.freya02.botcommands.api.application.slash.annotations.JDASlashCommand
import com.freya02.docs.DocSourceType

@CommandMarker
class DocsCommand : BaseDocCommand() {
    @JDASlashCommand(
        name = "docs",
        subcommand = "botcommands",
        description = "Shows the documentation for a class, a method or a field"
    )
    fun onSlashDocsBC(
        event: GuildSlashEvent,
        @AppOption(description = "The docs to search upon") sourceType: DocSourceType,
        @AppOption(
            description = "Name of the Java class",
            autocomplete = CommonDocsHandlers.CLASS_NAME_WITH_METHODS_AUTOCOMPLETE_NAME
        ) className: String,
        @AppOption(
            description = "Signature of the method / field name",
            autocomplete = CommonDocsHandlers.METHOD_NAME_OR_FIELD_BY_CLASS_AUTOCOMPLETE_NAME
        ) identifier: String?
    ) {
        onSlashDocs(event, sourceType, className, identifier)
    }

    @JDASlashCommand(
        name = "docs",
        subcommand = "jda",
        description = "Shows the documentation for a class, a method or a field"
    )
    fun onSlashDocsJDA(
        event: GuildSlashEvent,
        @AppOption(description = "The docs to search upon") sourceType: DocSourceType,
        @AppOption(
            description = "Name of the Java class",
            autocomplete = CommonDocsHandlers.CLASS_NAME_WITH_METHODS_AUTOCOMPLETE_NAME
        ) className: String,
        @AppOption(
            description = "Signature of the method / field name",
            autocomplete = CommonDocsHandlers.METHOD_NAME_OR_FIELD_BY_CLASS_AUTOCOMPLETE_NAME
        ) identifier: String?
    ) {
        onSlashDocs(event, sourceType, className, identifier)
    }

    @JDASlashCommand(
        name = "docs",
        subcommand = "java",
        description = "Shows the documentation for a class, a method or a field"
    )
    fun onSlashDocsJava(
        event: GuildSlashEvent,
        @AppOption(description = "The docs to search upon") sourceType: DocSourceType,
        @AppOption(
            description = "Name of the Java class",
            autocomplete = CommonDocsHandlers.CLASS_NAME_WITH_METHODS_AUTOCOMPLETE_NAME
        ) className: String,
        @AppOption(
            description = "Signature of the method / field name",
            autocomplete = CommonDocsHandlers.METHOD_NAME_OR_FIELD_BY_CLASS_AUTOCOMPLETE_NAME
        ) identifier: String?
    ) {
        onSlashDocs(event, sourceType, className, identifier)
    }

    private fun onSlashDocs(
        event: GuildSlashEvent,
        sourceType: DocSourceType,
        className: String,
        identifier: String?
    ) {
        val docIndex = docIndexMap[sourceType]
        if (identifier == null) {
            CommonDocsHandlers.handleClass(event, className, docIndex)
        } else if (identifier.contains("(")) { //prob a method
            CommonDocsHandlers.handleMethodDocs(event, className, identifier, docIndex)
        } else {
            CommonDocsHandlers.handleFieldDocs(event, className, identifier, docIndex)
        }
    }
}