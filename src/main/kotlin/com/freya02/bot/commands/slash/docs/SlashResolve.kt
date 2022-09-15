package com.freya02.bot.commands.slash.docs

import com.freya02.bot.commands.slash.docs.CommonDocsHandlers.Companion.transformResolveChain
import com.freya02.bot.docs.DocIndexMap
import com.freya02.bot.docs.cached.CachedClass
import com.freya02.bot.docs.cached.CachedField
import com.freya02.bot.docs.cached.CachedMethod
import com.freya02.botcommands.api.annotations.CommandMarker
import com.freya02.botcommands.api.commands.application.GlobalApplicationCommandManager
import com.freya02.botcommands.api.commands.application.annotations.AppDeclaration
import com.freya02.botcommands.api.commands.application.slash.GuildSlashEvent
import com.freya02.botcommands.api.components.Components
import com.freya02.docs.DocSourceType

private const val commandDescription = "Experimental - Resolves method/field calls into its final return type, and shows its documentation"
private const val chainArgDescription = "Chain of method/field calls, can also just be a class name. Each component is separated with an #"

@CommandMarker
class SlashResolve(private val docIndexMap: DocIndexMap, private val components: Components) : BaseDocCommand() {
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

        when (val doc = docIndex.resolveDoc(chain.transformResolveChain())) {
            is CachedClass -> CommonDocsHandlers.sendClass(event, false, doc, components)
            is CachedMethod -> CommonDocsHandlers.sendMethod(event, false, doc, components)
            is CachedField -> CommonDocsHandlers.sendField(event, false, doc, components)
        }
    }
}