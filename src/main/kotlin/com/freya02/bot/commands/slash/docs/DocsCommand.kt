package com.freya02.bot.commands.slash.docs

import com.freya02.bot.docs.DocIndexMap
import com.freya02.bot.docs.index.DocSuggestion
import com.freya02.bot.docs.index.DocSuggestion.Companion.mapToSuggestions
import com.freya02.botcommands.api.annotations.CommandMarker
import com.freya02.botcommands.api.commands.annotations.GeneratedOption
import com.freya02.botcommands.api.commands.application.annotations.AppOption
import com.freya02.botcommands.api.commands.application.slash.GuildSlashEvent
import com.freya02.botcommands.api.commands.application.slash.annotations.JDASlashCommand
import com.freya02.botcommands.api.components.Components
import com.freya02.docs.DocSourceType

@CommandMarker
class DocsCommand(private val docIndexMap: DocIndexMap, private val components: Components) : BaseDocCommand() {
    @JDASlashCommand(
        name = "docs",
        subcommand = "botcommands",
        description = "Shows the documentation for a class, a method or a field"
    )
    fun onSlashDocsBC(
        event: GuildSlashEvent,
        @GeneratedOption sourceType: DocSourceType,
        @AppOption(
            description = "Name of the Java class",
            autocomplete = CommonDocsHandlers.CLASS_NAME_AUTOCOMPLETE_NAME
        ) className: String,
        @AppOption(
            description = "Signature of the method / field name",
            autocomplete = CommonDocsHandlers.METHOD_OR_FIELD_BY_CLASS_AUTOCOMPLETE_NAME
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
        @GeneratedOption sourceType: DocSourceType,
        @AppOption(
            description = "Name of the Java class",
            autocomplete = CommonDocsHandlers.CLASS_NAME_AUTOCOMPLETE_NAME
        ) className: String,
        @AppOption(
            description = "Signature of the method / field name",
            autocomplete = CommonDocsHandlers.METHOD_OR_FIELD_BY_CLASS_AUTOCOMPLETE_NAME
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
        @GeneratedOption sourceType: DocSourceType,
        @AppOption(
            description = "Name of the Java class",
            autocomplete = CommonDocsHandlers.CLASS_NAME_AUTOCOMPLETE_NAME
        ) className: String,
        @AppOption(
            description = "Signature of the method / field name",
            autocomplete = CommonDocsHandlers.METHOD_OR_FIELD_BY_CLASS_AUTOCOMPLETE_NAME
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
        val docIndex = docIndexMap[sourceType]!!
        if (identifier == null) {
            CommonDocsHandlers.handleClass(event, className, docIndex, components) {
                return@handleClass classNameAutocomplete(docIndex, className, 100)
                    .map { DocSuggestion(it, it) }
            }
        } else if (identifier.contains("(")) { //prob a method
            CommonDocsHandlers.handleMethodDocs(event, className, identifier, docIndex, components) {
                return@handleMethodDocs methodOrFieldByClassAutocomplete(docIndex, className, identifier, 100).mapToSuggestions(className)
            }
        } else {
            CommonDocsHandlers.handleFieldDocs(event, className, identifier, docIndex, components) {
                return@handleFieldDocs methodOrFieldByClassAutocomplete(docIndex, className, identifier, 100).mapToSuggestions(className)
            }
        }
    }
}