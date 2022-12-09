package com.freya02.bot.commands.slash.docs

import com.freya02.bot.commands.slash.DeleteButtonListener
import com.freya02.bot.docs.DocIndexMap
import com.freya02.bot.docs.cached.CachedDoc
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
import dev.minn.jda.ktx.messages.Embed
import dev.minn.jda.ktx.messages.reply_
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.OnlineStatus
import net.dv8tion.jda.api.entities.ClientType
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.entities.UserSnowflake
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.exceptions.ErrorHandler
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback
import net.dv8tion.jda.api.interactions.commands.Command.Choice
import net.dv8tion.jda.api.interactions.components.ItemComponent
import net.dv8tion.jda.api.interactions.components.buttons.Button
import net.dv8tion.jda.api.interactions.components.selections.SelectMenu
import net.dv8tion.jda.api.requests.ErrorResponse
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder
import net.dv8tion.jda.api.utils.messages.MessageCreateData
import net.dv8tion.jda.api.utils.messages.MessageCreateRequest
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
                sendClass(event, true, doc)
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

        fun sendClass(event: IReplyCallback, ephemeral: Boolean, cachedDoc: CachedDoc) {
            event.reply(getDocMessageData(event.member!!, ephemeral, cachedDoc))
                .setEphemeral(ephemeral)
                .queue()
        }

        fun getDocMessageData(caller: Member, ephemeral: Boolean, cachedDoc: CachedDoc): MessageCreateData {
            return MessageCreateBuilder().apply {
                addEmbeds(cachedDoc.embed.withLink(cachedDoc, caller))
                addDocsSeeAlso(cachedDoc)
                addDocsActionRows(ephemeral, cachedDoc, caller)
            }.build()
        }

        private fun MessageEmbed.withLink(cachedDoc: CachedDoc, member: Member?): MessageEmbed {
            if (member == null) {
                logger.warn("Got a null member")
                return this
            }

            cachedDoc.javadocLink?.let { javadocLink ->
                if (member.getOnlineStatus(ClientType.MOBILE) != OnlineStatus.OFFLINE) {
                    return EmbedBuilder(this).addField("Link", javadocLink, false).build()
                }
            }

            return this
        }

        private fun MessageCreateRequest<*>.addDocsActionRows(
            ephemeral: Boolean,
            cachedDoc: CachedDoc,
            caller: UserSnowflake
        ) {
            val list: List<ItemComponent> = buildList {
                if (!ephemeral) add(DeleteButtonListener.getDeleteButton(caller))
                cachedDoc.sourceLink?.let { sourceLink -> add(Button.link(sourceLink, "Source")) }
            }

            if (list.isNotEmpty()) {
                addActionRow(list)
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

            sendClass(event, false, cachedMethod)
        }

        fun handleFieldDocs(event: GuildSlashEvent, className: String, identifier: String, docIndex: DocIndex, block: () -> List<DocSuggestion>) {
            val cachedField = docIndex.getFieldDoc(className, identifier) ?: run {
                val menu = getDocSuggestionsMenu(event, docIndex, block)

                event.reply(MessageCreateData.fromEditData(menu.get()))
                    .setEphemeral(true)
                    .queue()

                return
            }

            sendClass(event, false, cachedField)
        }

        fun buildDocSuggestionsMenu(docIndex: DocIndex, suggestions: List<DocSuggestion>, block: ChoiceMenuBuilder<DocSuggestion>.() -> Unit) =
            ChoiceMenuBuilder(suggestions)
                .setButtonContentSupplier { _, index -> ButtonContent.withString((index + 1).toString()) }
                .setTransformer { it.humanIdentifier }
                .setMaxEntriesPerPage(10)
                .setPaginatorSupplier { _, _, _, _ ->
                    return@setPaginatorSupplier Embed {
                        author {
                            name = when (docIndex.sourceType) {
                                DocSourceType.JAVA -> "Java Javadocs"
                                DocSourceType.JDA -> "JDA Javadocs"
                                DocSourceType.BOT_COMMANDS -> "BotCommands Javadocs"
                            }
                            iconUrl = when (docIndex.sourceType) {
                                DocSourceType.JAVA -> "https://assets.stickpng.com/images/58480979cef1014c0b5e4901.png"
                                DocSourceType.JDA -> "https://cdn.discordapp.com/icons/125227483518861312/8be466a3cdafc8591fcec4cdbb0eefc0.webp?size=128"
                                else -> null
                            }
                        }
                    }
                }
                .apply(block)
                .build()

        private fun getDocSuggestionsMenu(
            event: GuildSlashEvent,
            docIndex: DocIndex,
            block: () -> List<DocSuggestion>
        ) = buildDocSuggestionsMenu(docIndex, block()) {
            setTimeout(2, TimeUnit.MINUTES) { menu, _ ->
                menu.cleanup(event.context)
                event.hook
                    .editOriginalComponents()
                    .queue(null, ErrorHandler().ignore(ErrorResponse.UNKNOWN_MESSAGE, ErrorResponse.UNKNOWN_WEBHOOK))
            }

            setCallback { buttonEvent, entry ->
                event.hook.editOriginalComponents().queue()

                val identifier = entry.identifier
                val doc = when {
                    '(' in identifier -> docIndex.getMethodDoc(identifier)
                    '#' in identifier -> docIndex.getFieldDoc(identifier)
                    else -> docIndex.getClassDoc(identifier)
                }

                when (doc) {
                    null -> buttonEvent.reply_("This item is now invalid, try again", ephemeral = true).queue()
                    else -> sendClass(buttonEvent, false, doc)
                }
            }
        }

        fun String.transformResolveChain() = this.replace('.', '#')

        private fun MessageCreateRequest<*>.addDocsSeeAlso(cachedDoc: CachedDoc) {
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

                    addActionRow(selectionMenuBuilder.build())
                }
            }
        }
    }
}