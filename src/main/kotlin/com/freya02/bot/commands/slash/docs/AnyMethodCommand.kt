package com.freya02.bot.commands.slash.docs

import com.freya02.bot.commands.slash.docs.CommonDocsHandlers.Companion.ANY_METHOD_NAME_AUTOCOMPLETE_NAME
import com.freya02.bot.commands.slash.docs.controllers.SlashDocsController
import com.freya02.bot.docs.DocIndexMap
import com.freya02.bot.docs.index.DocSuggestion.Companion.mapToSuggestions
import com.freya02.botcommands.api.annotations.CommandMarker
import com.freya02.botcommands.api.commands.application.CommandScope
import com.freya02.botcommands.api.commands.application.GuildApplicationCommandManager
import com.freya02.botcommands.api.commands.application.annotations.AppDeclaration
import com.freya02.botcommands.api.commands.application.slash.GuildSlashEvent
import com.freya02.docs.DocSourceType
import dev.minn.jda.ktx.messages.reply_

@CommandMarker
class AnyMethodCommand(private val docIndexMap: DocIndexMap, private val slashDocsController: SlashDocsController) {
    @AppDeclaration
    fun declare(manager: GuildApplicationCommandManager) {
        manager.slashCommand("anymethod", CommandScope.GUILD) {
            description = "Shows the documentation for any method"

            DocSourceType.typesForGuild(manager.guild).forEach { sourceType ->
                subcommand(sourceType.cmdName) {
                    description = "Shows the documentation for any method"

                    generatedOption("sourceType") { sourceType }

                    option("fullSignature") {
                        description = "Full signature of the class + method"
                        autocompleteReference(ANY_METHOD_NAME_AUTOCOMPLETE_NAME)
                    }

                    function = ::onSlashAnyMethod
                }
            }
        }
    }

    @CommandMarker
    suspend fun onSlashAnyMethod(event: GuildSlashEvent, sourceType: DocSourceType, fullSignature: String) {
        val split = fullSignature.split("#".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        if (split.size != 2) {
            event.reply_(
                "You must supply a class name and a method signature, separated with a #, e.g. `Object#getClass()`",
                ephemeral = true
            ).queue()
            return
        }

        val docIndex = docIndexMap[sourceType]!!
        slashDocsController.handleMethodDocs(event, split[0], split[1], docIndex) {
            return@handleMethodDocs anyMethodNameAutocomplete(docIndex, fullSignature, 100).mapToSuggestions()
        }
    }
}