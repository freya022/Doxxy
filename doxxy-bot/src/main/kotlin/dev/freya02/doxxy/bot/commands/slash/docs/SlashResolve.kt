package dev.freya02.doxxy.bot.commands.slash.docs

import dev.freya02.doxxy.bot.commands.controllers.CommonDocsController
import dev.freya02.doxxy.bot.docs.DocIndexMap
import dev.freya02.doxxy.bot.docs.DocResolveChain
import dev.freya02.doxxy.bot.docs.DocSourceType
import dev.minn.jda.ktx.messages.reply_
import io.github.freya022.botcommands.api.annotations.CommandMarker
import io.github.freya022.botcommands.api.commands.annotations.Command
import io.github.freya022.botcommands.api.commands.application.provider.GuildApplicationCommandManager
import io.github.freya022.botcommands.api.commands.application.provider.GuildApplicationCommandProvider
import io.github.freya022.botcommands.api.commands.application.slash.GuildSlashEvent
import io.github.freya022.botcommands.api.commands.application.slash.options.builder.inlineClassOptionVararg

@Command
class SlashResolve(private val docIndexMap: DocIndexMap, private val commonDocsController: CommonDocsController) : GuildApplicationCommandProvider {
    override fun declareGuildApplicationCommands(manager: GuildApplicationCommandManager) {
        manager.slashCommand("resolve", function = null) {
            description = commandDescription

            DocSourceType.entries.forEach { sourceType ->
                subcommand(sourceType.cmdName, SlashResolve::onSlashResolve) {
                    description = commandDescription

                    generatedOption("sourceType") { sourceType }

                    inlineClassOptionVararg<DocResolveChain>("chain", amount = 10, requiredAmount = 1,
                        optionNameSupplier = { if (it == 0) "chain" else "chain_$it" }
                    ) {
                        description = chainArgDescription

                        autocompleteByName(CommonDocsHandlers.RESOLVE_AUTOCOMPLETE_NAME)
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
        val docIndex = docIndexMap[sourceType]
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