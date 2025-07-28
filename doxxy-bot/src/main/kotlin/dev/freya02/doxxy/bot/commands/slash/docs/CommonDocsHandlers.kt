package dev.freya02.doxxy.bot.commands.slash.docs

import dev.freya02.botcommands.jda.ktx.components.SelectOption
import dev.freya02.botcommands.jda.ktx.components.into
import dev.freya02.botcommands.jda.ktx.coroutines.await
import dev.freya02.botcommands.jda.ktx.messages.edit
import dev.freya02.botcommands.jda.ktx.messages.reply_
import dev.freya02.botcommands.jda.ktx.messages.toEditData
import dev.freya02.doxxy.bot.commands.controllers.CommonDocsController
import dev.freya02.doxxy.bot.commands.slash.docs.controllers.SlashDocsController
import dev.freya02.doxxy.bot.docs.DocIndexMap
import dev.freya02.doxxy.bot.docs.DocResolveChain
import dev.freya02.doxxy.bot.docs.DocSourceType
import dev.freya02.doxxy.bot.docs.index.DocSearchResult
import dev.freya02.doxxy.bot.examples.ExampleAPI
import dev.freya02.doxxy.bot.examples.ExamplePaginatorFactory
import dev.freya02.doxxy.docs.sections.SeeAlso.TargetType
import io.github.freya022.botcommands.api.commands.application.ApplicationCommand
import io.github.freya022.botcommands.api.commands.application.slash.autocomplete.annotations.AutocompleteHandler
import io.github.freya022.botcommands.api.commands.application.slash.autocomplete.annotations.CacheAutocomplete
import io.github.freya022.botcommands.api.components.SelectMenus
import io.github.freya022.botcommands.api.components.annotations.ComponentData
import io.github.freya022.botcommands.api.components.annotations.JDASelectMenuListener
import io.github.freya022.botcommands.api.components.event.StringSelectEvent
import io.github.freya022.botcommands.api.core.annotations.Handler
import io.github.oshai.kotlinlogging.KotlinLogging
import net.dv8tion.jda.api.entities.UserSnowflake
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.interactions.commands.Command.Choice
import kotlin.time.Duration.Companion.minutes

@Handler
class CommonDocsHandlers(
    // null if backend is disabled
    private val exampleApi: ExampleAPI?,
    private val docIndexMap: DocIndexMap,
    private val selectMenus: SelectMenus,
    private val commonDocsController: CommonDocsController,
    private val slashDocsController: SlashDocsController,
    private val examplePaginatorFactory: ExamplePaginatorFactory
) : ApplicationCommand() {
    @JDASelectMenuListener(name = SEE_ALSO_SELECT_LISTENER_NAME)
    suspend fun onSeeAlsoSelect(event: StringSelectEvent, @ComponentData owner: UserSnowflake, @ComponentData docSourceType: DocSourceType) {
        val values = event.selectedOptions.single().value.split(":")
        val targetType = TargetType.valueOf(values[0])
        val fullSignature = values[1]
        val doc = docIndexMap[docSourceType].let { index ->
            when (targetType) {
                TargetType.CLASS -> index.getClassDoc(fullSignature)
                TargetType.METHOD -> index.getMethodDoc(fullSignature)
                TargetType.FIELD -> index.getFieldDoc(fullSignature)
                else -> throw IllegalArgumentException("Invalid target type: $targetType")
            }
        }

        when (doc) {
            null -> event.reply_("This reference is not available anymore", ephemeral = true).queue()
            else -> when (owner) {
                //Caller is the same as original command caller, edit
                event.user -> commonDocsController.getDocMessageData(
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
        val language = example.contents.map { it.language }.let { languages ->
            val languageSelectMenu = selectMenus.stringSelectMenu().ephemeral {
                timeout(2.minutes)

                placeholder = "Select a language"
                options += languages.map { SelectOption(it, it) }
            }
            event.replyComponents(languageSelectMenu.into()).setEphemeral(true).await()

            languageSelectMenu.await().values.single()
        }

        val paginator = examplePaginatorFactory.fromInteraction(
            example.contents.single { it.language == language }.parts,
            event.user,
            ephemeral = true,
            event.hook
        )
        event.hook.editOriginal(paginator.createMessage().toEditData()).queue()
    }

    @CacheAutocomplete
    @AutocompleteHandler(name = CLASS_NAME_AUTOCOMPLETE_NAME, showUserInput = false)
    suspend fun onClassNameAutocomplete(
        event: CommandAutoCompleteInteractionEvent,
        sourceType: DocSourceType
    ): List<Choice> = docIndexMap.withDocIndex(sourceType) {
        classNameAutocomplete(this, event.focusedOption.value).toChoices()
    }

    @CacheAutocomplete(compositeKeys = ["className"])
    @AutocompleteHandler(name = METHOD_OR_FIELD_BY_CLASS_AUTOCOMPLETE_NAME, showUserInput = false)
    suspend fun onMethodOrFieldByClassAutocomplete(
        event: CommandAutoCompleteInteractionEvent,
        sourceType: DocSourceType,
        className: String
    ): List<Choice> = docIndexMap.withDocIndex(sourceType) {
        methodOrFieldByClassAutocomplete(this, className, event.focusedOption.value).searchResultToIdentifierChoices()
    }

    @CacheAutocomplete
    @AutocompleteHandler(name = SEARCH_AUTOCOMPLETE_NAME, showUserInput = false)
    suspend fun onSearchAutocomplete(
        event: CommandAutoCompleteInteractionEvent,
        sourceType: DocSourceType
    ): List<Choice> = docIndexMap.withDocIndex(sourceType) {
        searchAutocomplete(this, event.focusedOption.value).searchResultToFullIdentifierChoices()
    }

    @CacheAutocomplete
    @AutocompleteHandler(name = RESOLVE_AUTOCOMPLETE_NAME, showUserInput = false)
    suspend fun onResolveAutocomplete(
        event: CommandAutoCompleteInteractionEvent,
        sourceType: DocSourceType,
        chain: DocResolveChain
    ): List<Choice> = docIndexMap.withDocIndex(sourceType) {
        resolveDocAutocomplete(chain).searchResultToFullIdentifierChoices()
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
