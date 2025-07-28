package dev.freya02.doxxy.bot.commands.text.docs.controllers

import dev.freya02.botcommands.jda.ktx.messages.edit
import dev.freya02.botcommands.jda.ktx.messages.reply_
import dev.freya02.botcommands.jda.ktx.messages.toEditData
import dev.freya02.doxxy.bot.commands.controllers.CommonDocsController
import dev.freya02.doxxy.bot.docs.index.DocIndex
import dev.freya02.doxxy.bot.docs.index.DocSuggestion
import io.github.freya022.botcommands.api.components.event.ButtonEvent
import io.github.freya022.botcommands.api.core.service.annotations.BService
import io.github.freya022.botcommands.api.pagination.menu.buttonized.ButtonMenu
import net.dv8tion.jda.api.entities.UserSnowflake
import net.dv8tion.jda.api.exceptions.ErrorHandler
import net.dv8tion.jda.api.requests.ErrorResponse
import kotlin.time.Duration.Companion.minutes

@BService
class TextDocsController(private val commonDocsController: CommonDocsController) {
    suspend fun getDocSuggestionMenu(docIndex: DocIndex, suggestions: List<DocSuggestion>, user: UserSnowflake): ButtonMenu<DocSuggestion> {
        suspend fun callback(buttonEvent: ButtonEvent, entry: DocSuggestion) {
            val identifier = entry.fullIdentifier
            val doc = when {
                '(' in identifier -> docIndex.getMethodDoc(identifier)
                '#' in identifier -> docIndex.getFieldDoc(identifier)
                else -> docIndex.getClassDoc(identifier)
            }

            when (doc) {
                null -> buttonEvent.reply_("This item is now invalid, try again", ephemeral = true).queue()
                else -> {
                    val messageCreateData = commonDocsController.getDocMessageData(
                        originalHook = null,
                        caller = buttonEvent.member!!,
                        ephemeral = false,
                        showCaller = false,
                        cachedDoc = doc
                    )
                    messageCreateData.toEditData()
                        .edit(buttonEvent)
                        .queue()
                }
            }
        }

        return commonDocsController.buildDocSuggestionsMenu(docIndex, suggestions, user, ::callback) {
            useDeleteButton(true)

            setTimeout(2.minutes) { menu ->
                menu.cleanup()
                menu.message!!
                    .editMessageComponents()
                    .queue(
                        null,
                        ErrorHandler().ignore(ErrorResponse.UNKNOWN_MESSAGE, ErrorResponse.UNKNOWN_WEBHOOK)
                    )
            }
        }
    }
}
