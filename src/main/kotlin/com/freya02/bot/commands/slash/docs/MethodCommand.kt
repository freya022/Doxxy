package com.freya02.bot.commands.slash.docs

import com.freya02.bot.docs.DocIndexMap
import com.freya02.botcommands.api.annotations.CommandMarker
import com.freya02.botcommands.api.application.annotations.AppOption
import com.freya02.botcommands.api.application.slash.GuildSlashEvent
import com.freya02.botcommands.api.application.slash.annotations.JDASlashCommand
import com.freya02.docs.DocSourceType

@CommandMarker
class MethodCommand(private val docIndexMap: DocIndexMap) : BaseDocCommand() {
    @JDASlashCommand(
        name = "method",
        subcommand = "botcommands",
        description = "Shows the documentation for a method of a class"
    )
    fun onSlashMethodBC(
        event: GuildSlashEvent,
        @AppOption(description = "The docs to search upon") sourceType: DocSourceType,
        @AppOption(
            description = "Name of the Java class",
            autocomplete = CommonDocsHandlers.CLASS_NAME_WITH_METHODS_AUTOCOMPLETE_NAME
        ) className: String,
        @AppOption(
            description = "Signature of the method",
            autocomplete = CommonDocsHandlers.METHOD_NAME_BY_CLASS_AUTOCOMPLETE_NAME
        ) methodId: String
    ) {
        onSlashMethod(event, sourceType, className, methodId)
    }

    @JDASlashCommand(
        name = "method",
        subcommand = "jda",
        description = "Shows the documentation for a method of a class"
    )
    fun onSlashMethodJDA(
        event: GuildSlashEvent,
        @AppOption(description = "The docs to search upon") sourceType: DocSourceType,
        @AppOption(
            description = "Name of the Java class",
            autocomplete = CommonDocsHandlers.CLASS_NAME_WITH_METHODS_AUTOCOMPLETE_NAME
        ) className: String,
        @AppOption(
            description = "Signature of the method",
            autocomplete = CommonDocsHandlers.METHOD_NAME_BY_CLASS_AUTOCOMPLETE_NAME
        ) methodId: String
    ) {
        onSlashMethod(event, sourceType, className, methodId)
    }

    @JDASlashCommand(
        name = "method",
        subcommand = "java",
        description = "Shows the documentation for a method of a class"
    )
    fun onSlashMethodJava(
        event: GuildSlashEvent,
        @AppOption(description = "The docs to search upon") sourceType: DocSourceType,
        @AppOption(
            description = "Name of the Java class",
            autocomplete = CommonDocsHandlers.CLASS_NAME_WITH_METHODS_AUTOCOMPLETE_NAME
        ) className: String,
        @AppOption(
            description = "Signature of the method",
            autocomplete = CommonDocsHandlers.METHOD_NAME_BY_CLASS_AUTOCOMPLETE_NAME
        ) methodId: String
    ) {
        onSlashMethod(event, sourceType, className, methodId)
    }

    private fun onSlashMethod(
        event: GuildSlashEvent,
        sourceType: DocSourceType,
        className: String,
        methodId: String
    ) {
        val docIndex = docIndexMap[sourceType]!!
        CommonDocsHandlers.handleMethodDocs(event, className, methodId, docIndex)
    }
}