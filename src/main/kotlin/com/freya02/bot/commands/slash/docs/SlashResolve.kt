package com.freya02.bot.commands.slash.docs

import com.freya02.bot.commands.controllers.CommonDocsController
import com.freya02.bot.commands.slash.docs.CommonDocsHandlers.Companion.filterResolveChain
import com.freya02.bot.docs.DocIndexMap
import com.freya02.botcommands.api.annotations.CommandMarker
import com.freya02.botcommands.api.commands.application.CommandScope
import com.freya02.botcommands.api.commands.application.GuildApplicationCommandManager
import com.freya02.botcommands.api.commands.application.annotations.AppDeclaration
import com.freya02.botcommands.api.commands.application.slash.GuildSlashEvent
import com.freya02.docs.DocSourceType
import dev.minn.jda.ktx.messages.reply_

@CommandMarker
class SlashResolve(private val docIndexMap: DocIndexMap, private val commonDocsController: CommonDocsController) {
    @AppDeclaration
    fun declare(manager: GuildApplicationCommandManager) {
        manager.slashCommand("resolve", CommandScope.GUILD) {
            description = commandDescription

            DocSourceType.typesForGuild(manager.guild).forEach { sourceType ->
                subcommand(sourceType.cmdName) {
                    description = commandDescription

                    generatedOption("sourceType") { sourceType }

                    option("chain") {
                        description = chainArgDescription
                        varArgs = 10
                        requiredVarArgs = 1

                        autocompleteReference(CommonDocsHandlers.RESOLVE_AUTOCOMPLETE_NAME)
                    }

                    function = ::onSlashResolve
                }
            }
        }
    }

    @CommandMarker
    suspend fun onSlashResolve(
        event: GuildSlashEvent,
        sourceType: DocSourceType,
        chain: List<String?>
    ) {
        val docIndex = docIndexMap[sourceType]!!
        val docChain = chain.filterResolveChain()
        val doc = docIndex.resolveDoc(docChain) ?: let {
            event.reply_("Could not find documentation for `$chain`", ephemeral = true).queue()
            return
        }

        val chainString = when (docChain.size) {
            1 -> docChain.first()
            else -> docChain.first() + "#" + docChain.drop(1).joinToString("#") { it.substringAfter('#') }
        }

        commonDocsController.getDocMessageData(
            originalHook = event.hook,
            caller = event.member,
            ephemeral = false,
            showCaller = false,
            cachedDoc = doc,
            chain = chainString
        ).let { event.reply(it).setEphemeral(false).queue() }
    }

    companion object {
        private const val commandDescription =
            "Experimental - Resolves method/field calls into its final return type, and shows its documentation"
        private const val chainArgDescription =
            "Chain of method/field calls, can also just be a class name. Each component is separated with an #"
    }
}