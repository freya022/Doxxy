package com.freya02.bot.commands.text

import com.freya02.bot.commands.slash.docs.CommonDocsHandlers
import com.freya02.bot.docs.DocIndexMap
import com.freya02.bot.docs.index.DocIndex
import com.freya02.bot.docs.index.DocSuggestion
import com.freya02.bot.docs.index.DocSuggestion.Companion.mapToSuggestions
import com.freya02.bot.docs.index.DocType
import com.freya02.botcommands.api.BContext
import com.freya02.botcommands.api.annotations.CommandMarker
import com.freya02.botcommands.api.prefixed.BaseCommandEvent
import com.freya02.botcommands.api.prefixed.TextCommand
import com.freya02.botcommands.api.prefixed.annotations.JDATextCommand
import com.freya02.botcommands.api.prefixed.annotations.TextOption
import com.freya02.docs.DocSourceType
import dev.minn.jda.ktx.messages.reply_
import net.dv8tion.jda.api.exceptions.ErrorHandler
import net.dv8tion.jda.api.requests.ErrorResponse
import net.dv8tion.jda.api.utils.messages.MessageCreateData
import net.dv8tion.jda.api.utils.messages.MessageEditData
import java.util.concurrent.TimeUnit

@CommandMarker
class TextDocs(private val context: BContext, private val docIndexMap: DocIndexMap) : TextCommand() {
    @JDATextCommand(name = "docs", description = "Shows the documentation for a class, a method or a field")
    fun onTextDocs(
        event: BaseCommandEvent,
        @TextOption(name = "docs source", example = "JDA") docSourceType: DocSourceType?,
        @TextOption(name = "query", example = "Guild#ban") query: String
    ) {
        val docIndex = docIndexMap[docSourceType ?: DocSourceType.JDA]!!

        val suggestions = when {
            '#' in query || '.' in query -> { // Method or field
                docIndex.findAnySignatures(query, 25, DocType.FIELD, DocType.METHOD).mapToSuggestions()
            }
            else -> {
                docIndex.getClassDoc(query)?.let {
                    event.channel.sendMessage(CommonDocsHandlers.getDocMessageData(event.member, false, it)).queue()
                    return
                }

                docIndex.getClasses(query).map { DocSuggestion(it, it) }
            }
        }

        getDocSuggestionMenu(docIndex, suggestions)
            .let { MessageCreateData.fromEditData(it.get()) }
            .also { event.channel.sendMessage(it).queue() }
    }

    private fun getDocSuggestionMenu(docIndex: DocIndex, suggestions: List<DocSuggestion>) =
        CommonDocsHandlers.buildDocSuggestionsMenu(docIndex, suggestions) {
            setTimeout(2, TimeUnit.MINUTES) { menu, message ->
                menu.cleanup(context)
                message!!
                    .editMessageComponents()
                    .queue(
                        null,
                        ErrorHandler().ignore(ErrorResponse.UNKNOWN_MESSAGE, ErrorResponse.UNKNOWN_WEBHOOK)
                    )
            }

            setCallback { buttonEvent, entry ->
                val identifier = entry.identifier
                val doc = when {
                    '(' in identifier -> docIndex.getMethodDoc(identifier)
                    '#' in identifier -> docIndex.getFieldDoc(identifier)
                    else -> docIndex.getClassDoc(identifier)
                }

                when (doc) {
                    null -> buttonEvent.reply_("This item is now invalid, try again", ephemeral = true).queue()
                    else -> buttonEvent.editMessage(MessageEditData.fromCreateData(
                        CommonDocsHandlers.getDocMessageData(buttonEvent.member!!, false, doc)
                    )).queue()
                }
            }
        }
}