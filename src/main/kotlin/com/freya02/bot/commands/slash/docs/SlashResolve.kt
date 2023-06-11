package com.freya02.bot.commands.slash.docs

import com.freya02.bot.commands.controllers.CommonDocsController
import com.freya02.bot.docs.DocIndexMap
import com.freya02.bot.docs.DocResolveChain
import com.freya02.botcommands.api.annotations.CommandMarker
import com.freya02.botcommands.api.commands.annotations.Command
import com.freya02.botcommands.api.commands.application.CommandScope
import com.freya02.botcommands.api.commands.application.GuildApplicationCommandManager
import com.freya02.botcommands.api.commands.application.annotations.AppDeclaration
import com.freya02.botcommands.api.commands.application.slash.GuildSlashEvent
import com.freya02.docs.DocSourceType
import dev.minn.jda.ktx.messages.reply_

@Command
class SlashResolve(private val docIndexMap: DocIndexMap, private val commonDocsController: CommonDocsController) {
    @AppDeclaration
    fun declare(manager: GuildApplicationCommandManager) {
        manager.slashCommand("resolve", CommandScope.GUILD, null) {
            description = commandDescription

            DocSourceType.typesForGuild(manager.guild).forEach { sourceType ->
                subcommand(sourceType.cmdName, ::onSlashResolve) {
                    description = commandDescription

                    generatedOption("sourceType") { sourceType }

                    inlineClassOptionVararg<DocResolveChain>("chain", amount = 10, requiredAmount = 1,
                        optionNameSupplier = { if (it == 0) "chain" else "chain_$it" }
                    ) {
                        description = chainArgDescription

                        autocompleteReference(CommonDocsHandlers.RESOLVE_AUTOCOMPLETE_NAME)
                    }
                }
            }
        }
    }

    @CommandMarker
    suspend fun onSlashResolve(
        event: GuildSlashEvent,
        sourceType: DocSourceType,
        chain: DocResolveChain
    ) {
        val docIndex = docIndexMap[sourceType]!!
        val doc = docIndex.resolveDoc(chain) ?: let {
            event.reply_("Could not find documentation for `$chain`", ephemeral = true).queue()
            return
        }

        commonDocsController.getDocMessageData(
            originalHook = event.hook,
            caller = event.member,
            ephemeral = false,
            showCaller = false,
            cachedDoc = doc,
            chain = chain
        ).let { event.reply(it).setEphemeral(false).queue() }
    }

    companion object {
        private const val commandDescription =
            "Concatenates qualified signatures and shows the documentation of the last chain"
        private const val chainArgDescription =
            "Qualified signature of a method/field, shows methods if first chain start with #"
    }
}