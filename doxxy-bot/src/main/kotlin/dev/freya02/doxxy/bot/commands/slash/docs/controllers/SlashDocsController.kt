package dev.freya02.doxxy.bot.commands.slash.docs.controllers

import dev.freya02.doxxy.bot.commands.controllers.CommonDocsController
import dev.freya02.doxxy.bot.commands.slash.docs.searchAutocomplete
import dev.freya02.doxxy.bot.docs.DocIndexMap
import dev.freya02.doxxy.bot.docs.DocSourceType
import dev.freya02.doxxy.bot.docs.cached.CachedDoc
import dev.freya02.doxxy.bot.docs.index.DocIndex
import dev.freya02.doxxy.bot.docs.index.DocSuggestion
import dev.freya02.doxxy.bot.docs.index.DocSuggestion.Companion.mapToSuggestions
import io.github.freya022.botcommands.api.commands.application.slash.GuildSlashEvent
import io.github.freya022.botcommands.api.components.event.ButtonEvent
import io.github.freya022.botcommands.api.core.service.annotations.BService
import io.github.freya022.botcommands.api.core.utils.edit
import io.github.freya022.botcommands.api.core.utils.toEditData
import io.github.freya022.botcommands.api.pagination.menu.buttonized.ButtonMenu
import net.dv8tion.jda.api.exceptions.ErrorHandler
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback
import net.dv8tion.jda.api.requests.ErrorResponse
import kotlin.time.Duration.Companion.minutes

@BService
class SlashDocsController(private val commonDocsController: CommonDocsController, private val docIndexMap: DocIndexMap) {
    suspend fun sendClass(event: IReplyCallback, ephemeral: Boolean, cachedDoc: CachedDoc) {
        event.reply(commonDocsController.getDocMessageData(event.hook, event.member!!, ephemeral, false, cachedDoc))
            .setEphemeral(ephemeral)
            .queue()
    }

    suspend fun handleClass(event: GuildSlashEvent, className: String, docIndex: DocIndex, block: suspend () -> List<DocSuggestion>) {
        val cachedClass = docIndex.getClassDoc(className) ?: run {
            val menu = getDocSuggestionsMenu(event, docIndex, block)
            return event.reply(menu.getInitialMessage()).queue()
        }

        sendClass(event, false, cachedClass)
    }

    suspend fun handleMethodDocs(event: GuildSlashEvent, className: String, identifier: String, docIndex: DocIndex, block: suspend () -> List<DocSuggestion>) {
        val cachedMethod = docIndex.getMethodDoc(className, identifier) ?: run {
            val menu = getDocSuggestionsMenu(event, docIndex, block)
            return event.reply(menu.getInitialMessage()).queue()
        }

        sendClass(event, false, cachedMethod)
    }

    suspend fun handleFieldDocs(event: GuildSlashEvent, className: String, identifier: String, docIndex: DocIndex, block: suspend () -> List<DocSuggestion>) {
        val cachedField = docIndex.getFieldDoc(className, identifier) ?: run {
            val menu = getDocSuggestionsMenu(event, docIndex, block)
            return event.reply(menu.getInitialMessage()).queue()
        }

        sendClass(event, false, cachedField)
    }

    //Used by DocsCommand and SlashSearch
    suspend fun onSearchSlashCommand(event: GuildSlashEvent, sourceType: DocSourceType, query: String) {
        val docIndex = docIndexMap[sourceType]
        when {
            '(' in query && '#' in query -> {
                val (className, identifier) = query.split("#")
                handleMethodDocs(event, className, identifier, docIndex) {
                    searchAutocomplete(docIndex, query).mapToSuggestions()
                }
            }

            '#' in query -> {
                val (className, identifier) = query.split("#")
                handleFieldDocs(event, className, identifier, docIndex) {
                    searchAutocomplete(docIndex, query).mapToSuggestions()
                }
            }

            else -> handleClass(event, query, docIndex) {
                searchAutocomplete(docIndex, query).mapToSuggestions()
            }
        }
    }

    private suspend fun getDocSuggestionsMenu(
        event: GuildSlashEvent,
        docIndex: DocIndex,
        block: suspend () -> List<DocSuggestion>
    ): ButtonMenu<DocSuggestion> {
        suspend fun callback(buttonEvent: ButtonEvent, entry: DocSuggestion) {
            val identifier = entry.fullIdentifier
            val doc = when {
                '(' in identifier -> docIndex.getMethodDoc(identifier)
                '#' in identifier -> docIndex.getFieldDoc(identifier)
                else -> docIndex.getClassDoc(identifier)
            }

            when (doc) {
                null -> buttonEvent.editMessage("The docs were updated, please try again").queue()
                else -> {
                    val messageCreateData = commonDocsController.getDocMessageData(
                        event.hook,
                        buttonEvent.member!!,
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

        return commonDocsController.buildDocSuggestionsMenu(docIndex, block(), event.user, ::callback) {
            useDeleteButton(true)

            setTimeout(2.minutes) { menu ->
                menu.cleanup()
                event.hook
                    .deleteOriginal()
                    .queue(null, ErrorHandler().ignore(ErrorResponse.UNKNOWN_MESSAGE, ErrorResponse.UNKNOWN_WEBHOOK))
            }
        }
    }
}
