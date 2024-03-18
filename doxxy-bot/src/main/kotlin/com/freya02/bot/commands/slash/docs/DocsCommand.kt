package com.freya02.bot.commands.slash.docs

import com.freya02.bot.commands.slash.docs.CommonDocsHandlers.Companion.CLASS_NAME_AUTOCOMPLETE_NAME
import com.freya02.bot.commands.slash.docs.CommonDocsHandlers.Companion.METHOD_OR_FIELD_BY_CLASS_AUTOCOMPLETE_NAME
import com.freya02.bot.commands.slash.docs.controllers.SlashDocsController
import com.freya02.bot.docs.DocIndexMap
import com.freya02.bot.docs.index.DocSuggestion
import com.freya02.bot.docs.index.DocSuggestion.Companion.mapToSuggestions
import com.freya02.docs.DocSourceType
import io.github.freya022.botcommands.api.annotations.CommandMarker
import io.github.freya022.botcommands.api.commands.annotations.Command
import io.github.freya022.botcommands.api.commands.application.CommandScope
import io.github.freya022.botcommands.api.commands.application.provider.GuildApplicationCommandManager
import io.github.freya022.botcommands.api.commands.application.provider.GuildApplicationCommandProvider
import io.github.freya022.botcommands.api.commands.application.slash.GuildSlashEvent

@Command
class DocsCommand(private val docIndexMap: DocIndexMap, private val slashDocsController: SlashDocsController) : GuildApplicationCommandProvider {
    override fun declareGuildApplicationCommands(manager: GuildApplicationCommandManager) {
        manager.slashCommand("docs", CommandScope.GUILD, null) {
            description = "Shows the documentation"

            DocSourceType.typesForGuild(manager.guild).forEach { sourceType ->
                subcommand(sourceType.cmdName, DocsCommand::onSlashDocs) {
                    description = "Shows the documentation for a class, a method or a field"

                    generatedOption("sourceType") { sourceType }

                    option("className") {
                        description = "Name of the Java class"
                        autocompleteByName(CLASS_NAME_AUTOCOMPLETE_NAME)
                    }

                    option("identifier") {
                        description = "Signature of the method / field name"
                        autocompleteByName(METHOD_OR_FIELD_BY_CLASS_AUTOCOMPLETE_NAME)
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
