package com.freya02.bot.commands.text

import com.freya02.bot.commands.controllers.CommonDocsController
import com.freya02.bot.commands.text.docs.controllers.TextDocsController
import com.freya02.bot.docs.DocIndexMap
import com.freya02.bot.docs.index.DocSuggestion
import com.freya02.bot.docs.index.DocSuggestion.Companion.mapToSuggestions
import com.freya02.bot.docs.index.DocType
import com.freya02.botcommands.api.annotations.CommandMarker
import com.freya02.botcommands.api.commands.prefixed.BaseCommandEvent
import com.freya02.botcommands.api.commands.prefixed.TextCommand
import com.freya02.botcommands.api.commands.prefixed.annotations.JDATextCommand
import com.freya02.botcommands.api.commands.prefixed.annotations.TextOption
import com.freya02.docs.DocSourceType
import net.dv8tion.jda.api.utils.messages.MessageCreateData

@CommandMarker
class TextDocs(
    private val docIndexMap: DocIndexMap,
    private val commonDocsController: CommonDocsController,
    private val textDocsController: TextDocsController
) : TextCommand() {
    @JDATextCommand(name = "docs", description = "Shows the documentation for a class, a method or a field")
    fun onTextDocs(
        event: BaseCommandEvent,
        @TextOption(name = "docs source", example = "JDA") docSourceType: DocSourceType = DocSourceType.JDA,
        @TextOption(name = "query", example = "Guild#ban") query: String
    ) {
        val docIndex = docIndexMap[docSourceType]!!

        val suggestions = when {
            '#' in query || '.' in query -> { // Method or field
                docIndex.findAnySignatures(query, 25, DocType.FIELD, DocType.METHOD).mapToSuggestions()
            }
            else -> {
                docIndex.getClassDoc(query)?.let {
                    event.channel.sendMessage(commonDocsController.getDocMessageData(event.member, false, it)).queue()
                    return
                }

                docIndex.getClasses(query).map { DocSuggestion(it, it) }
            }
        }

        textDocsController.getDocSuggestionMenu(docIndex, suggestions)
            .let { MessageCreateData.fromEditData(it.get()) }
            .also { event.channel.sendMessage(it).queue() }
    }
}