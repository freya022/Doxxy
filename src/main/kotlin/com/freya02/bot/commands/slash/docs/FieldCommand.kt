package com.freya02.bot.commands.slash.docs

import com.freya02.bot.docs.DocIndexMap
import com.freya02.botcommands.api.annotations.CommandMarker
import com.freya02.botcommands.api.application.annotations.AppOption
import com.freya02.botcommands.api.application.slash.GuildSlashEvent
import com.freya02.botcommands.api.application.slash.annotations.JDASlashCommand
import com.freya02.docs.DocSourceType

@CommandMarker
class FieldCommand(private val docIndexMap: DocIndexMap) : BaseDocCommand() {
    @JDASlashCommand(
        name = "field",
        subcommand = "botcommands",
        description = "Shows the documentation for a field of a class"
    )
    fun onSlashFieldBC(
        event: GuildSlashEvent,
        @AppOption(description = "The docs to search upon") sourceType: DocSourceType,
        @AppOption(
            description = "The class to search the field in",
            autocomplete = CommonDocsHandlers.CLASS_NAME_WITH_FIELDS_AUTOCOMPLETE_NAME
        ) className: String,
        @AppOption(
            description = "Name of the field",
            autocomplete = CommonDocsHandlers.FIELD_NAME_BY_CLASS_AUTOCOMPLETE_NAME
        ) fieldName: String
    ) {
        onSlashField(event, sourceType, className, fieldName)
    }

    @JDASlashCommand(name = "field", subcommand = "jda", description = "Shows the documentation for a field of a class")
    fun onSlashFieldJDA(
        event: GuildSlashEvent,
        @AppOption(description = "The docs to search upon") sourceType: DocSourceType,
        @AppOption(
            description = "The class to search the field in",
            autocomplete = CommonDocsHandlers.CLASS_NAME_WITH_FIELDS_AUTOCOMPLETE_NAME
        ) className: String,
        @AppOption(
            description = "Name of the field",
            autocomplete = CommonDocsHandlers.FIELD_NAME_BY_CLASS_AUTOCOMPLETE_NAME
        ) fieldName: String
    ) {
        onSlashField(event, sourceType, className, fieldName)
    }

    @JDASlashCommand(
        name = "field",
        subcommand = "java",
        description = "Shows the documentation for a field of a class"
    )
    fun onSlashFieldJava(
        event: GuildSlashEvent,
        @AppOption(description = "The docs to search upon") sourceType: DocSourceType,
        @AppOption(
            description = "The class to search the field in",
            autocomplete = CommonDocsHandlers.CLASS_NAME_WITH_FIELDS_AUTOCOMPLETE_NAME
        ) className: String,
        @AppOption(
            description = "Name of the field",
            autocomplete = CommonDocsHandlers.FIELD_NAME_BY_CLASS_AUTOCOMPLETE_NAME
        ) fieldName: String
    ) {
        onSlashField(event, sourceType, className, fieldName)
    }

    private fun onSlashField(
        event: GuildSlashEvent,
        sourceType: DocSourceType,
        className: String,
        fieldName: String
    ) {
        val docIndex = docIndexMap[sourceType]!!
        CommonDocsHandlers.handleFieldDocs(event, className, fieldName, docIndex)
    }
}