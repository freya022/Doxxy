package com.freya02.bot.commands.slash.docs

import com.freya02.bot.commands.slash.docs.CommonDocsHandlers.Companion.CLASS_NAME_AUTOCOMPLETE_NAME
import com.freya02.bot.commands.slash.docs.CommonDocsHandlers.Companion.EXPERIMENTAL_SEARCH_AUTOCOMPLETE_NAME
import com.freya02.bot.commands.slash.docs.CommonDocsHandlers.Companion.METHOD_OR_FIELD_BY_CLASS_AUTOCOMPLETE_NAME
import com.freya02.bot.commands.slash.docs.CommonDocsHandlers.Companion.SEARCH_AUTOCOMPLETE_NAME
import com.freya02.bot.commands.slash.docs.controllers.SlashDocsController
import com.freya02.bot.docs.DocIndexMap
import com.freya02.bot.docs.index.DocSuggestion
import com.freya02.bot.docs.index.DocSuggestion.Companion.mapToSuggestions
import com.freya02.bot.docs.index.DocTypes
import com.freya02.botcommands.api.annotations.CommandMarker
import com.freya02.botcommands.api.commands.application.CommandScope
import com.freya02.botcommands.api.commands.application.GuildApplicationCommandManager
import com.freya02.botcommands.api.commands.application.annotations.AppDeclaration
import com.freya02.botcommands.api.commands.application.slash.GuildSlashEvent
import com.freya02.docs.DocSourceType

@CommandMarker
class DocsCommand(private val docIndexMap: DocIndexMap, private val slashDocsController: SlashDocsController) {
    @AppDeclaration
    fun declare(manager: GuildApplicationCommandManager) {
        manager.slashCommand("docs", CommandScope.GUILD) {
            description = "Shows the documentation"

            DocSourceType.typesForGuild(manager.guild).forEach { sourceType ->
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

            subcommandGroup("search") {
                DocSourceType.typesForGuild(manager.guild).forEach { sourceType ->
                    subcommand(sourceType.cmdName) {
                        description = "Searches the documentation for anything"

                        generatedOption("sourceType") { sourceType }

                        //Required for autocomplete
                        generatedOption(declaredName = "docTypes") { DocTypes.ANY }

                        option("query") {
                            description = "The docs to search for"
                            autocompleteReference(SEARCH_AUTOCOMPLETE_NAME)
                        }

                        function = slashDocsController::onSearchSlashCommand
                    }
                }
            }

            subcommandGroup("exp") {
                DocSourceType.typesForGuild(manager.guild).forEach { sourceType ->
                    subcommand(sourceType.cmdName) {
                        description = "Experimental - Searches the documentation for anything, with a custom sort"

                        generatedOption("sourceType") { sourceType }

                        //Required for autocomplete
                        generatedOption(declaredName = "docTypes") { DocTypes.ANY }

                        option("query") {
                            description = "The docs to search for"
                            autocompleteReference(EXPERIMENTAL_SEARCH_AUTOCOMPLETE_NAME)
                        }

                        function = slashDocsController::onSearchSlashCommand
                    }
                }
            }
        }
    }

    @CommandMarker
    suspend fun onSlashDocs(
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
                return@handleMethodDocs methodOrFieldByClassAutocomplete(docIndex, className, identifier, 100).mapToSuggestions()
            }
        } else {
            slashDocsController.handleFieldDocs(event, className, identifier, docIndex) {
                return@handleFieldDocs methodOrFieldByClassAutocomplete(docIndex, className, identifier, 100).mapToSuggestions()
            }
        }
    }
}
