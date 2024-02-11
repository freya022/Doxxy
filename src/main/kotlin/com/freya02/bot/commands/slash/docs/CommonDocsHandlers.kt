package com.freya02.bot.commands.slash.docs

import com.freya02.bot.commands.controllers.CommonDocsController
import com.freya02.bot.commands.slash.docs.controllers.SlashDocsController
import com.freya02.bot.docs.DocIndexMap
import com.freya02.bot.docs.DocResolveChain
import com.freya02.bot.docs.index.DocIndex
import com.freya02.bot.docs.index.DocSearchResult
import com.freya02.bot.examples.ExampleAPI
import com.freya02.docs.DocSourceType
import com.freya02.docs.data.TargetType
import dev.minn.jda.ktx.messages.reply_
import io.github.freya022.botcommands.api.commands.application.ApplicationCommand
import io.github.freya022.botcommands.api.commands.application.slash.autocomplete.annotations.AutocompleteHandler
import io.github.freya022.botcommands.api.commands.application.slash.autocomplete.annotations.CacheAutocomplete
import io.github.freya022.botcommands.api.components.annotations.JDASelectMenuListener
import io.github.freya022.botcommands.api.components.event.StringSelectEvent
import io.github.freya022.botcommands.api.core.annotations.Handler
import io.github.freya022.botcommands.api.core.utils.edit
import io.github.freya022.botcommands.api.core.utils.toEditData
import io.github.oshai.kotlinlogging.KotlinLogging
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.interactions.commands.Command.Choice

@Handler
class CommonDocsHandlers(
    // null if backend is disabled
    private val exampleApi: ExampleAPI?,
    private val docIndexMap: DocIndexMap,
    private val commonDocsController: CommonDocsController,
    private val slashDocsController: SlashDocsController
) : ApplicationCommand() {
    @JDASelectMenuListener(name = SEE_ALSO_SELECT_LISTENER_NAME)
    suspend fun onSeeAlsoSelect(event: StringSelectEvent, ownerId: Long, docSourceType: DocSourceType) {
        val values = event.selectedOptions.single().value.split(":")
        val targetType = TargetType.valueOf(values[0])
        val fullSignature = values[1]
        val doc = docIndexMap[docSourceType]?.let { index ->
            when (targetType) {
                TargetType.CLASS -> index.getClassDoc(fullSignature)
                TargetType.METHOD -> index.getMethodDoc(fullSignature)
                TargetType.FIELD -> index.getFieldDoc(fullSignature)
                else -> throw IllegalArgumentException("Invalid target type: $targetType")
            }
        }

        when (doc) {
            null -> event.reply_("This reference is not available anymore", ephemeral = true).queue()
            else -> when (ownerId) {
                //Caller is the same as original command caller, edit
                event.user.idLong -> commonDocsController.getDocMessageData(
                    event.hook,
                    event.member!!,
                    ephemeral = false,
                    showCaller = false,
                    cachedDoc = doc
                ).toEditData()
                    .edit(event)
                    .queue()

                else -> slashDocsController.sendClass(event, true, doc)
            }
        }
    }

    @JDASelectMenuListener(name = EXAMPLE_SELECT_LISTENER_NAME)
    suspend fun onExampleSelect(event: StringSelectEvent) {
        if (exampleApi == null) return

        val title = event.values.single()
        val example = exampleApi.getExampleByTitle(title)
            ?: return event.reply_("This example no longer exists", ephemeral = true).queue()

        event.reply_(example.contents.first().content, ephemeral = true).queue()
    }

    @CacheAutocomplete
    @AutocompleteHandler(name = CLASS_NAME_AUTOCOMPLETE_NAME, showUserInput = false)
    suspend fun onClassNameAutocomplete(
        event: CommandAutoCompleteInteractionEvent,
        sourceType: DocSourceType
    ): List<Choice> = withDocIndex(sourceType) {
        classNameAutocomplete(this, event.focusedOption.value).toChoices()
    }

    @CacheAutocomplete(compositeKeys = ["className"])
    @AutocompleteHandler(name = METHOD_OR_FIELD_BY_CLASS_AUTOCOMPLETE_NAME, showUserInput = false)
    suspend fun onMethodOrFieldByClassAutocomplete(
        event: CommandAutoCompleteInteractionEvent,
        sourceType: DocSourceType,
        className: String
    ): List<Choice> = withDocIndex(sourceType) {
        methodOrFieldByClassAutocomplete(this, className, event.focusedOption.value).searchResultToIdentifierChoices()
    }

    @CacheAutocomplete
    @AutocompleteHandler(name = SEARCH_AUTOCOMPLETE_NAME, showUserInput = false)
    suspend fun onSearchAutocomplete(
        event: CommandAutoCompleteInteractionEvent,
        sourceType: DocSourceType
    ): List<Choice> = withDocIndex(sourceType) {
        searchAutocomplete(this, event.focusedOption.value).searchResultToFullIdentifierChoices()
    }

    @CacheAutocomplete
    @AutocompleteHandler(name = RESOLVE_AUTOCOMPLETE_NAME, showUserInput = false)
    suspend fun onResolveAutocomplete(
        event: CommandAutoCompleteInteractionEvent,
        sourceType: DocSourceType,
        chain: DocResolveChain
    ): List<Choice> = withDocIndex(sourceType) {
        resolveDocAutocomplete(chain).searchResultToFullIdentifierChoices()
    }

    private inline fun withDocIndex(sourceType: DocSourceType, block: DocIndex.() -> List<Choice>): List<Choice> {
        val map = docIndexMap[sourceType] ?: return emptyList()
        return block(map)
    }

    companion object {
        private val logger = KotlinLogging.logger { }

        const val CLASS_NAME_AUTOCOMPLETE_NAME = "CommonDocsHandlers: className"
        const val METHOD_OR_FIELD_BY_CLASS_AUTOCOMPLETE_NAME = "CommonDocsHandlers: methodNameOrFieldByClass"
        const val SEARCH_AUTOCOMPLETE_NAME = "CommonDocsHandlers: search"
        const val RESOLVE_AUTOCOMPLETE_NAME = "CommonDocsHandlers: resolve"

        const val SEE_ALSO_SELECT_LISTENER_NAME = "CommonDocsHandlers: seeAlso"
        const val EXAMPLE_SELECT_LISTENER_NAME = "CommonDocsHandlers: examples"

        val AUTOCOMPLETE_NAMES = arrayOf(
            CLASS_NAME_AUTOCOMPLETE_NAME,
            METHOD_OR_FIELD_BY_CLASS_AUTOCOMPLETE_NAME,
            SEARCH_AUTOCOMPLETE_NAME,
            RESOLVE_AUTOCOMPLETE_NAME
        )

        fun Iterable<String>.toChoices() = this.map { Choice(it, it) }
        fun Iterable<DocSearchResult>.searchResultToFullIdentifierChoices() = this
            .filter { it.fullIdentifier.length <= Choice.MAX_STRING_VALUE_LENGTH }
            .map { Choice(it.humanClassIdentifier.tryAppendReturnType(it), it.fullIdentifier) }
        fun Iterable<DocSearchResult>.searchResultToIdentifierChoices() = this
            .filter { it.identifier.length <= Choice.MAX_STRING_VALUE_LENGTH }
            .map { Choice(it.humanIdentifier.tryAppendReturnType(it), it.identifier) }

        private fun String.tryAppendReturnType(searchResult: DocSearchResult): String = when {
            searchResult.returnType == null -> this
            this.length + ": ${searchResult.returnType}".length > Choice.MAX_NAME_LENGTH -> this
            else -> "$this: ${searchResult.returnType}"
        }
    }
}
