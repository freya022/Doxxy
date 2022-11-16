package com.freya02.bot.commands.slash.docs

import com.freya02.bot.commands.slash.DeleteButtonListener
import com.freya02.bot.docs.DocIndexMap
import com.freya02.bot.docs.cached.CachedClass
import com.freya02.bot.docs.cached.CachedDoc
import com.freya02.bot.docs.cached.CachedField
import com.freya02.bot.docs.cached.CachedMethod
import com.freya02.bot.docs.index.DocIndex
import com.freya02.bot.docs.index.DocResolveResult
import com.freya02.bot.docs.index.DocSearchResult
import com.freya02.bot.docs.index.DocSuggestion
import com.freya02.botcommands.api.Logging
import com.freya02.botcommands.api.application.ApplicationCommand
import com.freya02.botcommands.api.application.annotations.AppOption
import com.freya02.botcommands.api.application.slash.GuildSlashEvent
import com.freya02.botcommands.api.application.slash.autocomplete.annotations.AutocompletionHandler
import com.freya02.botcommands.api.application.slash.autocomplete.annotations.CacheAutocompletion
import com.freya02.botcommands.api.application.slash.autocomplete.annotations.CompositeKey
import com.freya02.botcommands.api.components.Components
import com.freya02.botcommands.api.components.annotations.JDASelectionMenuListener
import com.freya02.botcommands.api.components.event.StringSelectionEvent
import com.freya02.botcommands.api.pagination.menu.ChoiceMenuBuilder
import com.freya02.botcommands.api.utils.ButtonContent
import com.freya02.botcommands.api.utils.EmojiUtils
import com.freya02.docs.DocSourceType
import com.freya02.docs.data.TargetType
import dev.minn.jda.ktx.messages.reply_
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.OnlineStatus
import net.dv8tion.jda.api.entities.ClientType
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.exceptions.ErrorHandler
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback
import net.dv8tion.jda.api.interactions.commands.Command.Choice
import net.dv8tion.jda.api.interactions.components.ItemComponent
import net.dv8tion.jda.api.interactions.components.buttons.Button
import net.dv8tion.jda.api.interactions.components.selections.SelectMenu
import net.dv8tion.jda.api.requests.ErrorResponse
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction
import net.dv8tion.jda.api.utils.messages.MessageCreateData
import java.util.concurrent.TimeUnit

private val logger = Logging.getLogger()

class CommonDocsHandlers(private val docIndexMap: DocIndexMap) : ApplicationCommand() {
    @JDASelectionMenuListener(name = SEE_ALSO_SELECT_LISTENER_NAME)
    fun onSeeAlsoSelect(event: StringSelectionEvent) {
        val option = event.selectedOptions[0] //Forced to use 1
        val values = option.value.split(":")
        val targetType = TargetType.valueOf(values[0])
        val fullSignature = values[1]
        for (index in docIndexMap.values) {
            val doc = when (targetType) {
                TargetType.CLASS -> index.getClassDoc(fullSignature)
                TargetType.METHOD -> index.getMethodDoc(fullSignature)
                TargetType.FIELD -> index.getFieldDoc(fullSignature)
                else -> throw IllegalArgumentException("Invalid target type: $targetType")
            }
            if (doc != null) {
                when (targetType) {
                    TargetType.CLASS -> sendClass(event, true, doc as CachedClass)
                    TargetType.METHOD -> sendMethod(event, true, doc as CachedMethod)
                    TargetType.FIELD -> sendField(event, true, doc as CachedField)
                    else -> throw IllegalArgumentException("Invalid target type: $targetType")
                }
                break
            }
        }
    }

    @CacheAutocompletion
    @AutocompletionHandler(name = CLASS_NAME_AUTOCOMPLETE_NAME, showUserInput = false)
    fun onClassNameAutocomplete(
        event: CommandAutoCompleteInteractionEvent,
        @CompositeKey @AppOption sourceType: DocSourceType
    ): Collection<Choice> = withDocIndex(sourceType) {
        classNameAutocomplete(this, event.focusedOption.value).toChoices()
    }

    @CacheAutocompletion
    @AutocompletionHandler(name = CLASS_NAME_WITH_METHODS_AUTOCOMPLETE_NAME, showUserInput = false)
    fun onClassNameWithMethodsAutocomplete(
        event: CommandAutoCompleteInteractionEvent,
        @CompositeKey @AppOption sourceType: DocSourceType
    ): Collection<Choice> = withDocIndex(sourceType) {
        classNameWithMethodsAutocomplete(this, event.focusedOption.value).toChoices()
    }

    @CacheAutocompletion
    @AutocompletionHandler(name = CLASS_NAME_WITH_FIELDS_AUTOCOMPLETE_NAME, showUserInput = false)
    fun onClassNameWithFieldsAutocomplete(
        event: CommandAutoCompleteInteractionEvent,
        @CompositeKey @AppOption sourceType: DocSourceType
    ): Collection<Choice> = withDocIndex(sourceType) {
        classNameWithFieldsAutocomplete(this, event.focusedOption.value).toChoices()
    }

    @CacheAutocompletion
    @AutocompletionHandler(name = METHOD_NAME_BY_CLASS_AUTOCOMPLETE_NAME, showUserInput = false)
    fun onMethodNameByClassAutocomplete(
        event: CommandAutoCompleteInteractionEvent,
        @CompositeKey @AppOption sourceType: DocSourceType,
        @CompositeKey @AppOption className: String
    ): Collection<Choice> = withDocIndex(sourceType) {
        methodNameByClassAutocomplete(this, className, event.focusedOption.value).searchResultToChoices { it.humanIdentifier }
    }

    @CacheAutocompletion
    @AutocompletionHandler(name = ANY_METHOD_NAME_AUTOCOMPLETE_NAME, showUserInput = false)
    fun onAnyMethodNameAutocomplete(
        event: CommandAutoCompleteInteractionEvent,
        @CompositeKey @AppOption sourceType: DocSourceType
    ): Collection<Choice> = withDocIndex(sourceType) {
        anyMethodNameAutocomplete(this, event.focusedOption.value).searchResultToChoices { it.humanClassIdentifier }
    }

    @CacheAutocompletion
    @AutocompletionHandler(name = FIELD_NAME_BY_CLASS_AUTOCOMPLETE_NAME, showUserInput = false)
    fun onFieldNameByClassAutocomplete(
        event: CommandAutoCompleteInteractionEvent,
        @CompositeKey @AppOption sourceType: DocSourceType,
        @CompositeKey @AppOption className: String
    ): Collection<Choice> = withDocIndex(sourceType) {
        fieldNameByClassAutocomplete(this, className, event.focusedOption.value).searchResultToChoices { it.humanIdentifier }
    }

    @CacheAutocompletion
    @AutocompletionHandler(name = ANY_FIELD_NAME_AUTOCOMPLETE_NAME, showUserInput = false)
    fun onAnyFieldNameAutocomplete(
        event: CommandAutoCompleteInteractionEvent,
        @CompositeKey @AppOption sourceType: DocSourceType
    ): Collection<Choice> = withDocIndex(sourceType) {
        anyFieldNameAutocomplete(this, event.focusedOption.value).searchResultToChoices { it.humanClassIdentifier }
    }

    @CacheAutocompletion
    @AutocompletionHandler(name = METHOD_OR_FIELD_BY_CLASS_AUTOCOMPLETE_NAME, showUserInput = false)
    fun onMethodOrFieldByClassAutocomplete(
        event: CommandAutoCompleteInteractionEvent,
        @CompositeKey @AppOption sourceType: DocSourceType,
        @CompositeKey @AppOption className: String
    ): Collection<Choice> = withDocIndex(sourceType) {
        methodOrFieldByClassAutocomplete(this, className, event.focusedOption.value).searchResultToChoices { it.humanIdentifier }
    }

    @CacheAutocompletion
    @AutocompletionHandler(name = RESOLVE_AUTOCOMPLETE_NAME, showUserInput = false)
    fun onResolveAutocomplete(
        event: CommandAutoCompleteInteractionEvent,
        @CompositeKey @AppOption sourceType: DocSourceType
    ): Collection<Choice> = withDocIndex(sourceType) {
        resolveDocAutocomplete(event.focusedOption.value.transformResolveChain()).resolveResultToChoices()
    }

    private fun withDocIndex(sourceType: DocSourceType, block: DocIndex.() -> List<Choice>): List<Choice> {
        val map = docIndexMap[sourceType] ?: return emptyList()
        return block(map)
    }

    private fun Iterable<String>.toChoices() = this.map { Choice(it, it) }
    private fun Iterable<DocSearchResult>.searchResultToChoices(nameExtractor: (DocSearchResult) -> String) = this
        .filter { it.identifierOrFullIdentifier.length <= Choice.MAX_STRING_VALUE_LENGTH }
        .map { Choice(nameExtractor(it), it.identifierOrFullIdentifier) }
    private fun Iterable<DocResolveResult>.resolveResultToChoices() = this
        .filter { it.value.length <= Choice.MAX_STRING_VALUE_LENGTH }
        .map { Choice(it.name, it.value) }

    companion object {
        const val CLASS_NAME_AUTOCOMPLETE_NAME = "CommonDocsHandlers: className"
        const val CLASS_NAME_WITH_METHODS_AUTOCOMPLETE_NAME = "CommonDocsHandlers: classNameWithMethods"
        const val CLASS_NAME_WITH_FIELDS_AUTOCOMPLETE_NAME = "CommonDocsHandlers: classNameWithFields"
        const val METHOD_NAME_BY_CLASS_AUTOCOMPLETE_NAME = "CommonDocsHandlers: methodNameByClass"
        const val ANY_METHOD_NAME_AUTOCOMPLETE_NAME = "CommonDocsHandlers: anyMethodName"
        const val FIELD_NAME_BY_CLASS_AUTOCOMPLETE_NAME = "FieldCommand: fieldNameByClass"
        const val ANY_FIELD_NAME_AUTOCOMPLETE_NAME = "CommonDocsHandlers: anyFieldName"
        const val METHOD_OR_FIELD_BY_CLASS_AUTOCOMPLETE_NAME = "CommonDocsHandlers: methodNameOrFieldByClass"
        const val RESOLVE_AUTOCOMPLETE_NAME = "CommonDocsHandlers: resolve"

        const val SEE_ALSO_SELECT_LISTENER_NAME = "CommonDocsHandlers: seeAlso"

        val AUTOCOMPLETE_NAMES = arrayOf(
            CLASS_NAME_AUTOCOMPLETE_NAME,
            CLASS_NAME_WITH_METHODS_AUTOCOMPLETE_NAME,
            CLASS_NAME_WITH_FIELDS_AUTOCOMPLETE_NAME,
            METHOD_NAME_BY_CLASS_AUTOCOMPLETE_NAME,
            ANY_METHOD_NAME_AUTOCOMPLETE_NAME,
            FIELD_NAME_BY_CLASS_AUTOCOMPLETE_NAME,
            ANY_FIELD_NAME_AUTOCOMPLETE_NAME,
            METHOD_OR_FIELD_BY_CLASS_AUTOCOMPLETE_NAME,
            RESOLVE_AUTOCOMPLETE_NAME
        )

        fun sendClass(event: IReplyCallback, ephemeral: Boolean, cachedClass: CachedClass) {
            event.replyEmbeds(cachedClass.embed.withLink(event, cachedClass))
                .addSeeAlso(cachedClass)
                .also { addActionRows(ephemeral, event, cachedClass, it) }
                .setEphemeral(ephemeral)
                .queue()
        }

        fun sendMethod(event: IReplyCallback, ephemeral: Boolean, cachedMethod: CachedMethod) {
            event.replyEmbeds(cachedMethod.embed.withLink(event, cachedMethod))
                .addSeeAlso(cachedMethod)
                .also { addActionRows(ephemeral, event, cachedMethod, it) }
                .setEphemeral(ephemeral)
                .queue()
        }

        fun sendField(event: IReplyCallback, ephemeral: Boolean, cachedField: CachedField) {
            event.replyEmbeds(cachedField.embed.withLink(event, cachedField))
                .addSeeAlso(cachedField)
                .also { addActionRows(ephemeral, event, cachedField, it) }
                .setEphemeral(ephemeral)
                .queue()
        }

        private fun MessageEmbed.withLink(event: IReplyCallback, cachedDoc: CachedDoc): MessageEmbed {
            cachedDoc.javadocLink?.let { javadocLink ->
                val member = event.member ?: run {
                    logger.warn("Got a null member")
                    return@let
                }

                if (member.getOnlineStatus(ClientType.MOBILE) != OnlineStatus.OFFLINE) {
                    return EmbedBuilder(this).addField("Link", javadocLink, false).build()
                }
            }

            return this
        }

        private fun addActionRows(
            ephemeral: Boolean,
            event: IReplyCallback,
            cachedDoc: CachedDoc,
            it: ReplyCallbackAction
        ) {
            val list: List<ItemComponent> = buildList {
                if (!ephemeral) add(DeleteButtonListener.getDeleteButton(event.user))
                cachedDoc.sourceLink?.let { sourceLink -> add(Button.link(sourceLink, "Source")) }
            }

            if (list.isNotEmpty()) {
                it.addActionRow(list)
            }
        }

        fun handleClass(event: GuildSlashEvent, className: String, docIndex: DocIndex, block: () -> List<DocSuggestion>) {
            val cachedClass = docIndex.getClassDoc(className) ?: run {
                val menu = getDocSuggestionsMenu(event, docIndex, block)

                event.reply(MessageCreateData.fromEditData(menu.get()))
                    .setEphemeral(true)
                    .queue()

                return
            }

            sendClass(event, false, cachedClass)
        }

        fun handleMethodDocs(event: GuildSlashEvent, className: String, identifier: String, docIndex: DocIndex, block: () -> List<DocSuggestion>) {
            val cachedMethod = docIndex.getMethodDoc(className, identifier) ?: run {
                val menu = getDocSuggestionsMenu(event, docIndex, block)

                event.reply(MessageCreateData.fromEditData(menu.get()))
                    .setEphemeral(true)
                    .queue()

                return
            }

            sendMethod(event, false, cachedMethod)
        }

        fun handleFieldDocs(event: GuildSlashEvent, className: String, identifier: String, docIndex: DocIndex, block: () -> List<DocSuggestion>) {
            val cachedField = docIndex.getFieldDoc(className, identifier) ?: run {
                val menu = getDocSuggestionsMenu(event, docIndex, block)

                event.reply(MessageCreateData.fromEditData(menu.get()))
                    .setEphemeral(true)
                    .queue()

                return
            }

            sendField(event, false, cachedField)
        }

        private fun getDocSuggestionsMenu(
            event: GuildSlashEvent,
            docIndex: DocIndex,
            block: () -> List<DocSuggestion>
        ) = ChoiceMenuBuilder(block())
            .setButtonContentSupplier { _, index -> ButtonContent.withString((index + 1).toString()) }
            .setTransformer { it.humanIdentifier }
            .setTimeout(2, TimeUnit.MINUTES) { _, _ ->
                event.hook
                    .editOriginalComponents()
                    .queue(null, ErrorHandler().ignore(ErrorResponse.UNKNOWN_MESSAGE, ErrorResponse.UNKNOWN_WEBHOOK))
            }
            .setCallback { buttonEvent, entry ->
                event.hook.editOriginalComponents().queue()

                val identifier = entry.identifier
                val doc = when {
                    '(' in identifier -> docIndex.getMethodDoc(identifier)
                    '#' in identifier -> docIndex.getFieldDoc(identifier)
                    else -> docIndex.getClassDoc(identifier)
                }

                when (doc) {
                    is CachedClass -> sendClass(buttonEvent, false, doc)
                    is CachedMethod -> sendMethod(buttonEvent, false, doc)
                    is CachedField -> sendField(buttonEvent, false, doc)
                    else -> buttonEvent.reply_("This item is now invalid, try again", ephemeral = true).queue()
                }
            }
            .build()

        fun String.transformResolveChain() = this.replace('.', '#')

        private fun ReplyCallbackAction.addSeeAlso(cachedDoc: CachedDoc): ReplyCallbackAction {
            cachedDoc.seeAlsoReferences.let { referenceList ->
                if (referenceList.any { it.targetType != TargetType.UNKNOWN }) {
                    val selectionMenuBuilder = Components.stringSelectionMenu(SEE_ALSO_SELECT_LISTENER_NAME)
                        .timeout(15, TimeUnit.MINUTES)
                        .setPlaceholder("See also")
                    for (reference in referenceList) {
                        if (reference.targetType != TargetType.UNKNOWN) {
                            val optionValue = reference.targetType.name + ":" + reference.fullSignature
                            if (optionValue.length > SelectMenu.ID_MAX_LENGTH) {
                                logger.warn(
                                    "Option value was too large ({}) for: '{}'",
                                    optionValue.length,
                                    optionValue
                                )

                                continue
                            }

                            selectionMenuBuilder.addOption(
                                reference.text,
                                optionValue,
                                EmojiUtils.resolveJDAEmoji("clipboard")
                            )
                        }
                    }

                    return addActionRow(selectionMenuBuilder.build())
                }
            }

            return this
        }
    }
}