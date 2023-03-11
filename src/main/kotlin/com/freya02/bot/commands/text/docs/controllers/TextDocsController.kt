package com.freya02.bot.commands.text.docs.controllers

import com.freya02.bot.commands.controllers.CommonDocsController
import com.freya02.bot.docs.index.DocIndex
import com.freya02.bot.docs.index.DocSuggestion
import com.freya02.botcommands.api.core.annotations.BService
import dev.minn.jda.ktx.messages.reply_
import kotlinx.coroutines.runBlocking
import net.dv8tion.jda.api.entities.UserSnowflake
import net.dv8tion.jda.api.exceptions.ErrorHandler
import net.dv8tion.jda.api.requests.ErrorResponse
import net.dv8tion.jda.api.utils.messages.MessageEditData
import java.util.concurrent.TimeUnit

@BService
class TextDocsController(private val commonDocsController: CommonDocsController) {
    suspend fun getDocSuggestionMenu(docIndex: DocIndex, suggestions: List<DocSuggestion>, user: UserSnowflake) =
        commonDocsController.buildDocSuggestionsMenu(docIndex, suggestions, user) {
            useDeleteButton(true)

            setTimeout(2, TimeUnit.MINUTES) { menu, message ->
                menu.cleanup()
                message!!
                    .editMessageComponents()
                    .queue(
                        null,
                        ErrorHandler().ignore(ErrorResponse.UNKNOWN_MESSAGE, ErrorResponse.UNKNOWN_WEBHOOK)
                    )
            }

            setCallback { buttonEvent, entry ->
                val identifier = entry.fullIdentifier
                val doc = runBlocking {
                    when {
                        '(' in identifier -> docIndex.getMethodDoc(identifier)
                        '#' in identifier -> docIndex.getFieldDoc(identifier)
                        else -> docIndex.getClassDoc(identifier)
                    }
                }

                when (doc) {
                    null -> buttonEvent.reply_("This item is now invalid, try again", ephemeral = true).queue()
                    else -> buttonEvent.editMessage(
                        MessageEditData.fromCreateData(
                        commonDocsController.getDocMessageData(buttonEvent.member!!,
                            ephemeral = false,
                            showCaller = false,
                            cachedDoc = doc
                        )
                    )).queue()
                }
            }
        }
}
