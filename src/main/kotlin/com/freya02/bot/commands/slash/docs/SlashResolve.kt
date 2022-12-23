package com.freya02.bot.commands.slash.docs

import com.freya02.bot.commands.slash.docs.CommonDocsHandlers.Companion.transformResolveChain
import com.freya02.bot.commands.slash.docs.controllers.SlashDocsController
import com.freya02.bot.docs.DocIndexMap
import com.freya02.botcommands.api.annotations.CommandMarker
import com.freya02.botcommands.api.commands.application.GlobalApplicationCommandManager
import com.freya02.botcommands.api.commands.application.annotations.AppDeclaration
import com.freya02.botcommands.api.commands.application.slash.GuildSlashEvent
import com.freya02.docs.DocSourceType
import dev.minn.jda.ktx.messages.reply_

@CommandMarker
class SlashResolve(private val docIndexMap: DocIndexMap, private val slashDocsController: SlashDocsController){
    @AppDeclaration
    fun declare(manager: GlobalApplicationCommandManager) {
        manager.slashCommand("resolve") {
            description = commandDescription

            DocSourceType.values().forEach { sourceType ->
                subcommand(sourceType.cmdName) {
                    description = commandDescription

                    generatedOption("sourceType") { sourceType }

                    option("chain") {
                        description = chainArgDescription
                        autocompleteReference(CommonDocsHandlers.RESOLVE_AUTOCOMPLETE_NAME)
                    }

                    function = ::onSlashResolve
                }
            }
        }
    }

    @CommandMarker
    fun onSlashResolve(
        event: GuildSlashEvent,
        sourceType: DocSourceType,
        chain: String
    ) {
        val docIndex = docIndexMap[sourceType]!!

        val doc = docIndex.resolveDoc(chain.transformResolveChain()) ?: let {
            event.reply_("Could not find documentation for `$chain`", ephemeral = true).queue()
            return
        }
        slashDocsController.sendClass(event, false, doc)
    }

    companion object {
        private const val commandDescription =
            "Experimental - Resolves method/field calls into its final return type, and shows its documentation"
        private const val chainArgDescription =
            "Chain of method/field calls, can also just be a class name. Each component is separated with an #"
    }
}