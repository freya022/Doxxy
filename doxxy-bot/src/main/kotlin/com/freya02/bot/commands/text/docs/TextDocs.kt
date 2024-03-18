package com.freya02.bot.commands.text.docs

import com.freya02.bot.commands.controllers.CommonDocsController
import com.freya02.bot.commands.text.docs.controllers.TextDocsController
import com.freya02.bot.docs.DocIndexMap
import com.freya02.bot.docs.index.DocSuggestion
import com.freya02.bot.docs.index.DocSuggestion.Companion.mapToSuggestions
import com.freya02.bot.docs.index.DocTypes
import com.freya02.docs.DocSourceType
import io.github.freya022.botcommands.api.commands.annotations.Command
import io.github.freya022.botcommands.api.commands.text.BaseCommandEvent
import io.github.freya022.botcommands.api.commands.text.TextCommand
import io.github.freya022.botcommands.api.commands.text.annotations.JDATextCommandVariation
import io.github.freya022.botcommands.api.commands.text.annotations.TextOption
import io.github.freya022.botcommands.api.core.utils.send

@Command
class TextDocs(
    private val docIndexMap: DocIndexMap,
    private val commonDocsController: CommonDocsController,
    private val textDocsController: TextDocsController
) : TextCommand() {
    @JDATextCommandVariation(path = ["docs"], description = "Shows the documentation for a class, a method or a field")
    suspend fun onTextDocs(
        event: BaseCommandEvent,
        @TextOption(name = "docs source", example = "JDA") docSourceType: DocSourceType = DocSourceType.JDA,
        @TextOption(name = "query", example = "Guild#ban") query: String
    ) {
        val docIndex = docIndexMap[docSourceType]!!

        val suggestions = when {
            '#' in query || '.' in query -> { // Method or field
                docIndex.findAnySignatures(query, 25, DocTypes.IDENTIFIERS).mapToSuggestions()
            }
            else -> {
                docIndex.getClassDoc(query)?.let {
                    event.channel.sendMessage(commonDocsController.getDocMessageData(
                        null,
                        event.member,
                        ephemeral = false,
                        showCaller = false,
                        cachedDoc = it
                    )).queue()
                    return
                }

                docIndex.getClasses(query).map { DocSuggestion(it, it) }
            }
        }

        val docSuggestionMenu = textDocsController.getDocSuggestionMenu(docIndex, suggestions, event.author)
        docSuggestionMenu
            .getInitialMessage()
            .send(event.channel)
            .queue { docSuggestionMenu.message = it }
    }
}
