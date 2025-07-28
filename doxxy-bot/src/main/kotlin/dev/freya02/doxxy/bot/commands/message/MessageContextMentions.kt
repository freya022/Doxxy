package dev.freya02.doxxy.bot.commands.message

import dev.freya02.botcommands.jda.ktx.messages.reply_
import dev.freya02.botcommands.jda.ktx.messages.suppressContentWarning
import dev.freya02.doxxy.bot.docs.mentions.DocMentionController
import io.github.freya022.botcommands.api.commands.annotations.Command
import io.github.freya022.botcommands.api.commands.application.ApplicationCommand
import io.github.freya022.botcommands.api.commands.application.context.annotations.JDAMessageCommand
import io.github.freya022.botcommands.api.commands.application.context.message.GuildMessageEvent
import net.dv8tion.jda.api.exceptions.ErrorHandler
import net.dv8tion.jda.api.requests.ErrorResponse

@Command
class MessageContextMentions(private val docMentionController: DocMentionController) : ApplicationCommand() {
    @JDAMessageCommand(name = "Find docs")
    suspend fun onMessageContextFindDocs(event: GuildMessageEvent) {
        val docMatches = suppressContentWarning { docMentionController.processMentions(event.target.contentRaw) }
        if (docMatches.isEmpty) return event.reply_("Could not match any docs", ephemeral = true).queue()

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
