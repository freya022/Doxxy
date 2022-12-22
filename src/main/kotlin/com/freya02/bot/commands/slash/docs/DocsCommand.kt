package com.freya02.bot.commands.slash.docs

import com.freya02.bot.commands.slash.docs.CommonDocsHandlers.Companion.CLASS_NAME_AUTOCOMPLETE_NAME
import com.freya02.bot.commands.slash.docs.CommonDocsHandlers.Companion.METHOD_OR_FIELD_BY_CLASS_AUTOCOMPLETE_NAME
import com.freya02.bot.commands.slash.docs.controllers.SlashDocsController
import com.freya02.bot.docs.DocIndexMap
import com.freya02.bot.docs.index.DocSuggestion
import com.freya02.bot.docs.index.DocSuggestion.Companion.mapToSuggestions
import com.freya02.botcommands.api.annotations.CommandMarker
import com.freya02.botcommands.api.commands.application.ApplicationCommand
import com.freya02.botcommands.api.commands.application.GlobalApplicationCommandManager
import com.freya02.botcommands.api.commands.application.annotations.AppDeclaration
import com.freya02.botcommands.api.commands.application.slash.GuildSlashEvent
import com.freya02.docs.DocSourceType

@CommandMarker
class DocsCommand(private val docIndexMap: DocIndexMap, private val slashDocsController: SlashDocsController) : ApplicationCommand() {
    @AppDeclaration
    fun declare(manager: GlobalApplicationCommandManager) {
        manager.slashCommand("docs") {
            description = "Shows the documentation for a class, a method or a field"

            DocSourceType.values().forEach { sourceType ->
                subcommand(sourceType.cmdName) {
                    description = "Shows the documentation for a class, a method or a field"

                    generatedOption("sourceType") { sourceType }

                    option("className") {
                        description = "Name of the Java class"
                        autocompleteReference(CLASS_NAME_AUTOCOMPLETE_NAME)
                    }

                    option("identifier") {
                        description = "Signature of the method / field name"
                        autocompleteReference(METHOD_OR_FIELD_BY_CLASS_AUTOCOMPLETE_NAME)
                    }

                    function = ::onSlashDocs
                }
            }
        }
    }

    @CommandMarker
    fun onSlashDocs(
        event: GuildSlashEvent,
        sourceType: DocSourceType,
        className: String,
        identifier: String?
    ) {
        val docIndex = docIndexMap[sourceType]!!
        if (identifier == null) {
            slashDocsController.handleClass(event, className, docIndex) {
                return@handleClass classNameAutocomplete(docIndex, className, 100)
                    .map { DocSuggestion(it, it) }
            }
        } else if (identifier.contains("(")) { //prob a method
            slashDocsController.handleMethodDocs(event, className, identifier, docIndex) {
                return@handleMethodDocs methodOrFieldByClassAutocomplete(docIndex, className, identifier, 100).mapToSuggestions(className)
            }
        } else {
            slashDocsController.handleFieldDocs(event, className, identifier, docIndex) {
                return@handleFieldDocs methodOrFieldByClassAutocomplete(docIndex, className, identifier, 100).mapToSuggestions(className)
            }
        }
    }
}