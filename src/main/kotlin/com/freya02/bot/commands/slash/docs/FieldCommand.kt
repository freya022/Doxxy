package com.freya02.bot.commands.slash.docs

import com.freya02.bot.commands.slash.docs.controllers.SlashDocsController
import com.freya02.bot.docs.DocIndexMap
import com.freya02.bot.docs.index.DocSuggestion.Companion.mapToSuggestions
import com.freya02.botcommands.api.annotations.CommandMarker
import com.freya02.botcommands.api.commands.application.ApplicationCommand
import com.freya02.botcommands.api.commands.application.GlobalApplicationCommandManager
import com.freya02.botcommands.api.commands.application.annotations.AppDeclaration
import com.freya02.botcommands.api.commands.application.slash.GuildSlashEvent
import com.freya02.docs.DocSourceType

@CommandMarker
class FieldCommand(private val docIndexMap: DocIndexMap, private val slashDocsController: SlashDocsController) : ApplicationCommand() {
    @AppDeclaration
    fun declare(manager: GlobalApplicationCommandManager) {
        manager.slashCommand("field") {
            description = "Shows the documentation for a field of a class"

            DocSourceType.values().forEach { sourceType ->
                subcommand(sourceType.cmdName) {
                    description = "Shows the documentation for a field of a class"

                    generatedOption("sourceType") { sourceType }

                    option("className") {
                        description = "The class to search the field in"
                        autocompleteReference(CommonDocsHandlers.CLASS_NAME_WITH_FIELDS_AUTOCOMPLETE_NAME)
                    }

                    option("fieldName") {
                        description = "Name of the field"
                        autocompleteReference(CommonDocsHandlers.FIELD_NAME_BY_CLASS_AUTOCOMPLETE_NAME)
                    }

                    function = ::onSlashField
                }
            }
        }
    }

    @CommandMarker
    fun onSlashField(
        event: GuildSlashEvent,
        sourceType: DocSourceType,
        className: String,
        fieldName: String
    ) {
        val docIndex = docIndexMap[sourceType]!!
        slashDocsController.handleFieldDocs(event, className, fieldName, docIndex) {
            return@handleFieldDocs fieldNameByClassAutocomplete(docIndex, className, fieldName, 100).mapToSuggestions(className)
        }
    }
}