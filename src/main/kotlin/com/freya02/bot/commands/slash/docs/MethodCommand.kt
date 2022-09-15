package com.freya02.bot.commands.slash.docs

import com.freya02.bot.docs.DocIndexMap
import com.freya02.bot.docs.index.DocSuggestion.Companion.mapToSuggestions
import com.freya02.botcommands.api.annotations.CommandMarker
import com.freya02.botcommands.api.commands.application.ApplicationCommand
import com.freya02.botcommands.api.commands.application.GlobalApplicationCommandManager
import com.freya02.botcommands.api.commands.application.annotations.AppDeclaration
import com.freya02.botcommands.api.commands.application.slash.GuildSlashEvent
import com.freya02.botcommands.api.components.Components
import com.freya02.docs.DocSourceType

@CommandMarker
class MethodCommand(private val docIndexMap: DocIndexMap, private val components: Components) : ApplicationCommand() {
    @AppDeclaration
    fun declare(manager: GlobalApplicationCommandManager) {
        manager.slashCommand("method") {
            description = "Shows the documentation for a method of a class"

            DocSourceType.values().forEach { sourceType ->
                subcommand(sourceType.cmdName) {
                    description = "Shows the documentation for a method of a class"

                    generatedOption("sourceType") { sourceType }

                    option("className") {
                        description = "Name of the Java class"
                        autocompleteReference(CommonDocsHandlers.CLASS_NAME_WITH_METHODS_AUTOCOMPLETE_NAME)
                    }

                    option("methodSignature") {
                        description = "Signature of the method"
                        autocompleteReference(CommonDocsHandlers.METHOD_NAME_BY_CLASS_AUTOCOMPLETE_NAME)
                    }

                    function = ::onSlashMethod
                }
            }
        }
    }

    @CommandMarker
    fun onSlashMethod(
        event: GuildSlashEvent,
        sourceType: DocSourceType,
        className: String,
        methodSignature: String
    ) {
        val docIndex = docIndexMap[sourceType]!!
        CommonDocsHandlers.handleMethodDocs(event, className, methodSignature, docIndex, components) {
            return@handleMethodDocs methodNameByClassAutocomplete(docIndex, className, methodSignature, 100).mapToSuggestions(className)
        }
    }
}