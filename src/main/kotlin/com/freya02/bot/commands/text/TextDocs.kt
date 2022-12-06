package com.freya02.bot.commands.text

import com.freya02.bot.commands.slash.docs.CommonDocsHandlers
import com.freya02.bot.docs.DocIndexMap
import com.freya02.bot.docs.cached.CachedClass
import com.freya02.bot.docs.cached.CachedField
import com.freya02.bot.docs.cached.CachedMethod
import com.freya02.bot.docs.index.DocIndex
import com.freya02.bot.docs.index.DocSuggestion
import com.freya02.bot.docs.index.DocSuggestion.Companion.mapToSuggestions
import com.freya02.bot.docs.index.DocType
import com.freya02.botcommands.api.annotations.CommandMarker
import com.freya02.botcommands.api.pagination.menu.ChoiceMenuBuilder
import com.freya02.botcommands.api.prefixed.BaseCommandEvent
import com.freya02.botcommands.api.prefixed.TextCommand
import com.freya02.botcommands.api.prefixed.annotations.JDATextCommand
import com.freya02.botcommands.api.prefixed.annotations.TextOption
import com.freya02.botcommands.api.utils.ButtonContent
import com.freya02.docs.DocSourceType
import dev.minn.jda.ktx.messages.reply_
import net.dv8tion.jda.api.exceptions.ErrorHandler
import net.dv8tion.jda.api.requests.ErrorResponse
import net.dv8tion.jda.api.utils.messages.MessageCreateData
import java.util.concurrent.TimeUnit

@CommandMarker
class TextDocs(private val docIndexMap: DocIndexMap) : TextCommand() {
    @JDATextCommand(name = "docs", description = "Shows the documentation for a class, a method or a field")
    fun onTextDocs(
        event: BaseCommandEvent,
        @TextOption(name = "docs source", example = "JDA") docSourceType: DocSourceType?,
        @TextOption(name = "query", example = "Guild#ban") query: String
    ) {
        val docIndex = docIndexMap[docSourceType ?: DocSourceType.JDA]!!

        if ('#' in query || '.' in query) {
            // Method or field
            docIndex.findAnySignatures(query, 25, DocType.FIELD, DocType.METHOD)
                .mapToSuggestions()
                .let { suggestions -> getDocSuggestionMenu(docIndex, suggestions) }
                .also { event.channel.sendMessage(MessageCreateData.fromEditData(it.get())).queue() }
        } else {
            docIndex.getClasses(query)
                .map { DocSuggestion(it, it) }
                .let { suggestions -> getDocSuggestionMenu(docIndex, suggestions) }
                .also { event.channel.sendMessage(MessageCreateData.fromEditData(it.get())).queue() }
        }
    }

    private fun getDocSuggestionMenu(docIndex: DocIndex, suggestions: List<DocSuggestion>) =
        ChoiceMenuBuilder(suggestions)
            .setButtonContentSupplier { _, index -> ButtonContent.withString((index + 1).toString()) }
            .setTransformer { it.humanIdentifier }
            .setTimeout(2, TimeUnit.MINUTES) { _, message ->
                message!!
                    .editMessageComponents()
                    .queue(
                        null,
                        ErrorHandler().ignore(ErrorResponse.UNKNOWN_MESSAGE, ErrorResponse.UNKNOWN_WEBHOOK)
                    )
            }
            .setCallback { buttonEvent, entry ->
                buttonEvent.message.delete().queue();

                val identifier = entry.identifier
                val doc = when {
                    '(' in identifier -> docIndex.getMethodDoc(identifier)
                    '#' in identifier -> docIndex.getFieldDoc(identifier)
                    else -> docIndex.getClassDoc(identifier)
                }

                when (doc) {
                    is CachedClass -> CommonDocsHandlers.sendClass(buttonEvent, false, doc)
                    is CachedMethod -> CommonDocsHandlers.sendMethod(buttonEvent, false, doc)
                    is CachedField -> CommonDocsHandlers.sendField(buttonEvent, false, doc)
                    else -> buttonEvent.reply_("This item is now invalid, try again", ephemeral = true)
                        .queue()
                }
            }
            .build()
}