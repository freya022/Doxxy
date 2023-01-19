package com.freya02.bot.commands.slash.docs

import com.freya02.bot.commands.slash.docs.CommonDocsHandlers.Companion.ANY_FIELD_NAME_AUTOCOMPLETE_NAME
import com.freya02.bot.commands.slash.docs.controllers.SlashDocsController
import com.freya02.bot.docs.DocIndexMap
import com.freya02.bot.docs.index.DocSuggestion.Companion.mapToSuggestions
import com.freya02.botcommands.api.annotations.CommandMarker
import com.freya02.botcommands.api.commands.application.GlobalApplicationCommandManager
import com.freya02.botcommands.api.commands.application.annotations.AppDeclaration
import com.freya02.botcommands.api.commands.application.slash.GuildSlashEvent
import com.freya02.docs.DocSourceType
import dev.minn.jda.ktx.messages.reply_

@CommandMarker
class AnyFieldCommand(private val docIndexMap: DocIndexMap, private val slashDocsController: SlashDocsController) {
    @AppDeclaration
    fun declare(manager: GlobalApplicationCommandManager) {
        manager.slashCommand("anyfield") {
            description = "Shows the documentation for any field"

            DocSourceType.values().forEach { sourceType ->
                subcommand(sourceType.cmdName) {
                    description = "Shows the documentation for any field"

                    generatedOption("sourceType") { sourceType }

                    option("fullSignature") {
                        description = "Full signature of the class + field"
                        autocompleteReference(ANY_FIELD_NAME_AUTOCOMPLETE_NAME)
                    }

                    function = ::onSlashAnyField
                }
            }
        }
    }

    @CommandMarker
    suspend fun onSlashAnyField(event: GuildSlashEvent, sourceType: DocSourceType, fullSignature: String) {
        val split = fullSignature.split("#")
        if (split.size != 2) {
            event.reply_(
                "You must supply a class name and a field name, separated with a #, e.g. `Integer#MAX_VALUE`",
                ephemeral = true
            ).queue()
            return
        }

        val docIndex = docIndexMap[sourceType]!!
        slashDocsController.handleFieldDocs(event, split[0], split[1], docIndex) {
            return@handleFieldDocs anyFieldNameAutocomplete(docIndex, fullSignature, 100).mapToSuggestions()
        }
    }
}