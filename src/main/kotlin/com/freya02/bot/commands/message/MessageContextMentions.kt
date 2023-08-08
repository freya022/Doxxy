package com.freya02.bot.commands.message

import com.freya02.bot.docs.mentions.DocMentionController
import com.freya02.botcommands.api.commands.annotations.Command
import com.freya02.botcommands.api.commands.application.ApplicationCommand
import com.freya02.botcommands.api.commands.application.context.annotations.JDAMessageCommand
import com.freya02.botcommands.api.commands.application.context.message.GuildMessageEvent
import com.freya02.botcommands.api.core.utils.suppressContentWarning
import dev.minn.jda.ktx.messages.reply_
import net.dv8tion.jda.api.exceptions.ErrorHandler
import net.dv8tion.jda.api.requests.ErrorResponse

@Command
class MessageContextMentions(private val docMentionController: DocMentionController) : ApplicationCommand() {
    @JDAMessageCommand(name = "Find docs")
    suspend fun onMessageContextFindDocs(event: GuildMessageEvent) {
        val docMatches = suppressContentWarning { docMentionController.processMentions(event.target.contentRaw) }
        if (docMatches.isEmpty) {
            event.reply_("Could not match any docs", ephemeral = true).queue()
            return
        }

        val hook = event.hook
        val message = docMentionController.createDocsMenuMessage(
            docMatches,
            event.user,
            timeoutCallback = {
                hook.deleteOriginal().queue(null, ErrorHandler().ignore(ErrorResponse.UNKNOWN_MESSAGE))
            })

        event.reply(message).queue()
    }
}
