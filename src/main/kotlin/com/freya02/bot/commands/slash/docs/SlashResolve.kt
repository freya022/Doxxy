package com.freya02.bot.commands.slash.docs

import com.freya02.bot.commands.slash.docs.CommonDocsHandlers.Companion.transformResolveChain
import com.freya02.bot.docs.DocIndexMap
import com.freya02.botcommands.api.annotations.CommandMarker
import com.freya02.botcommands.api.application.annotations.AppOption
import com.freya02.botcommands.api.application.slash.GuildSlashEvent
import com.freya02.botcommands.api.application.slash.annotations.JDASlashCommand
import com.freya02.docs.DocSourceType
import dev.minn.jda.ktx.messages.reply_

@CommandMarker
class SlashResolve(private val docIndexMap: DocIndexMap) : BaseDocCommand() {
    @JDASlashCommand(
        name = "resolve",
        subcommand = "botcommands",
        description = commandDescription
    )
    fun onSlashResolveBC(
        event: GuildSlashEvent,
        @AppOption(description = sourceTypeArgDescription) sourceType: DocSourceType,
        @AppOption(
            description = chainArgDescription,
            autocomplete = CommonDocsHandlers.RESOLVE_AUTOCOMPLETE_NAME
        ) chain: String
    ) {
        onSlashResolve(event, sourceType, chain)
    }

    @JDASlashCommand(
        name = "resolve",
        subcommand = "jda",
        description = commandDescription
    )
    fun onSlashResolveJDA(
        event: GuildSlashEvent,
        @AppOption(description = sourceTypeArgDescription) sourceType: DocSourceType,
        @AppOption(
            description = chainArgDescription,
            autocomplete = CommonDocsHandlers.RESOLVE_AUTOCOMPLETE_NAME
        ) chain: String
    ) {
        onSlashResolve(event, sourceType, chain)
    }

    @JDASlashCommand(
        name = "resolve",
        subcommand = "java",
        description = commandDescription
    )
    fun onSlashResolveJava(
        event: GuildSlashEvent,
        @AppOption(description = sourceTypeArgDescription) sourceType: DocSourceType,
        @AppOption(
            description = chainArgDescription,
            autocomplete = CommonDocsHandlers.RESOLVE_AUTOCOMPLETE_NAME
        ) chain: String
    ) {
        onSlashResolve(event, sourceType, chain)
    }

    private fun onSlashResolve(
        event: GuildSlashEvent,
        sourceType: DocSourceType,
        chain: String
    ) {
        val docIndex = docIndexMap[sourceType]!!

        val doc = docIndex.resolveDoc(chain.transformResolveChain()) ?: let {
            event.reply_("Could not find documentation for `$chain`", ephemeral = true).queue()
            return
        }
        CommonDocsHandlers.sendClass(event, false, doc)
    }

    companion object {
        private const val commandDescription =
            "Experimental - Resolves method/field calls into its final return type, and shows its documentation"
        private const val sourceTypeArgDescription = "The docs to search upon"
        private const val chainArgDescription =
            "Chain of method/field calls, can also just be a class name. Each component is separated with an #"
    }
}