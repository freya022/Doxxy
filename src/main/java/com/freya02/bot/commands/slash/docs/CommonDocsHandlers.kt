package com.freya02.bot.commands.slash.docs

import com.freya02.bot.commands.slash.DeleteButtonListener
import com.freya02.bot.docs.DocIndexMap
import com.freya02.bot.docs.cached.CachedClass
import com.freya02.bot.docs.cached.CachedDoc
import com.freya02.bot.docs.cached.CachedField
import com.freya02.bot.docs.cached.CachedMethod
import com.freya02.bot.docs.index.DocIndex
import com.freya02.botcommands.api.Logging
import com.freya02.botcommands.api.application.ApplicationCommand
import com.freya02.botcommands.api.application.annotations.AppOption
import com.freya02.botcommands.api.application.slash.GuildSlashEvent
import com.freya02.botcommands.api.application.slash.autocomplete.annotations.AutocompletionHandler
import com.freya02.botcommands.api.application.slash.autocomplete.annotations.CacheAutocompletion
import com.freya02.botcommands.api.application.slash.autocomplete.annotations.CompositeKey
import com.freya02.botcommands.api.components.Components
import com.freya02.botcommands.api.components.annotations.JDASelectionMenuListener
import com.freya02.botcommands.api.components.event.SelectionEvent
import com.freya02.botcommands.api.utils.EmojiUtils
import com.freya02.docs.DocSourceType
import com.freya02.docs.data.TargetType
import dev.minn.jda.ktx.messages.reply_
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import net.dv8tion.jda.api.interactions.components.selections.SelectMenu
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction
import java.util.concurrent.TimeUnit

private val LOGGER = Logging.getLogger()

class CommonDocsHandlers : ApplicationCommand() {
    private val docIndexMap: DocIndexMap = DocIndexMap.getInstance()

    @JDASelectionMenuListener(name = SEE_ALSO_SELECT_LISTENER_NAME)
    fun onSeeAlsoSelect(event: SelectionEvent) {
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
        event: CommandAutoCompleteInteractionEvent?,
        @CompositeKey @AppOption sourceType: DocSourceType
    ): Collection<String> {
        return docIndexMap[sourceType]?.simpleNameList ?: return listOf()
    }

    @CacheAutocompletion
    @AutocompletionHandler(name = CLASS_NAME_WITH_METHODS_AUTOCOMPLETE_NAME, showUserInput = false)
    fun onClassNameWithMethodsAutocomplete(
        event: CommandAutoCompleteInteractionEvent?,
        @CompositeKey @AppOption sourceType: DocSourceType
    ): Collection<String> {
        return docIndexMap[sourceType]?.classesWithMethods ?: return listOf()
    }

    @CacheAutocompletion
    @AutocompletionHandler(name = CLASS_NAME_WITH_FIELDS_AUTOCOMPLETE_NAME, showUserInput = false)
    fun onClassNameWithFieldsAutocomplete(
        event: CommandAutoCompleteInteractionEvent?,
        @CompositeKey @AppOption sourceType: DocSourceType
    ): Collection<String> {
        return docIndexMap[sourceType]?.classesWithFields ?: return listOf()
    }

    @CacheAutocompletion
    @AutocompletionHandler(name = METHOD_NAME_BY_CLASS_AUTOCOMPLETE_NAME, showUserInput = false)
    fun onMethodNameByClassAutocomplete(
        event: CommandAutoCompleteInteractionEvent?,
        @CompositeKey @AppOption sourceType: DocSourceType,
        @CompositeKey @AppOption className: String?
    ): Collection<String> {
        return docIndexMap[sourceType]?.getMethodDocSuggestions(className) ?: return listOf()
    }

    @CacheAutocompletion
    @AutocompletionHandler(name = ANY_METHOD_NAME_AUTOCOMPLETE_NAME, showUserInput = false)
    fun onAnyMethodNameAutocomplete(
        event: CommandAutoCompleteInteractionEvent?,
        @CompositeKey @AppOption sourceType: DocSourceType
    ): Collection<String> {
        val set = docIndexMap[sourceType]?.methodDocSuggestions ?: return listOf()

        //TODO real fix hopefully
        return set.filter { s: String -> s.length <= OptionData.MAX_CHOICE_VALUE_LENGTH }
    }

    @CacheAutocompletion
    @AutocompletionHandler(name = FIELD_NAME_AUTOCOMPLETE_NAME, showUserInput = false)
    fun onFieldNameAutocomplete(
        event: CommandAutoCompleteInteractionEvent?,
        @CompositeKey @AppOption sourceType: DocSourceType,
        @CompositeKey @AppOption className: String?
    ): Collection<String> {
        return docIndexMap[sourceType]?.getFieldDocSuggestions(className) ?: return listOf()
    }

    @CacheAutocompletion
    @AutocompletionHandler(name = ANY_FIELD_NAME_AUTOCOMPLETE_NAME, showUserInput = false)
    fun onAnyFieldNameAutocomplete(
        event: CommandAutoCompleteInteractionEvent?,
        @CompositeKey @AppOption sourceType: DocSourceType
    ): Collection<String> {
        return docIndexMap[sourceType]?.fieldDocSuggestions ?: return listOf()
    }

    @CacheAutocompletion
    @AutocompletionHandler(name = METHOD_NAME_OR_FIELD_BY_CLASS_AUTOCOMPLETE_NAME, showUserInput = false)
    fun onMethodNameOrFieldByClassAutocomplete(
        event: CommandAutoCompleteInteractionEvent?,
        @CompositeKey @AppOption sourceType: DocSourceType,
        @CompositeKey @AppOption className: String?
    ): Collection<String> {
        return docIndexMap[sourceType]?.getMethodAndFieldDocSuggestions(className) ?: return listOf()
    }

    companion object {
        const val CLASS_NAME_AUTOCOMPLETE_NAME = "CommonDocsHandlers: className"
        const val CLASS_NAME_WITH_METHODS_AUTOCOMPLETE_NAME = "CommonDocsHandlers: classNameWithMethods"
        const val CLASS_NAME_WITH_FIELDS_AUTOCOMPLETE_NAME = "CommonDocsHandlers: classNameWithFields"
        const val METHOD_NAME_BY_CLASS_AUTOCOMPLETE_NAME = "CommonDocsHandlers: methodNameByClass"
        const val ANY_METHOD_NAME_AUTOCOMPLETE_NAME = "CommonDocsHandlers: anyMethodName"
        const val FIELD_NAME_AUTOCOMPLETE_NAME = "FieldCommand: fieldName"
        const val ANY_FIELD_NAME_AUTOCOMPLETE_NAME = "CommonDocsHandlers: anyFieldName"
        const val METHOD_NAME_OR_FIELD_BY_CLASS_AUTOCOMPLETE_NAME = "CommonDocsHandlers: methodNameOrFieldByClass"
        const val SEE_ALSO_SELECT_LISTENER_NAME = "CommonDocsHandlers: seeAlso"

        @JvmField
        val AUTOCOMPLETE_NAMES = arrayOf(
            CLASS_NAME_AUTOCOMPLETE_NAME,
            CLASS_NAME_WITH_METHODS_AUTOCOMPLETE_NAME,
            CLASS_NAME_WITH_FIELDS_AUTOCOMPLETE_NAME,
            METHOD_NAME_BY_CLASS_AUTOCOMPLETE_NAME,
            ANY_METHOD_NAME_AUTOCOMPLETE_NAME,
            FIELD_NAME_AUTOCOMPLETE_NAME,
            ANY_FIELD_NAME_AUTOCOMPLETE_NAME,
            METHOD_NAME_OR_FIELD_BY_CLASS_AUTOCOMPLETE_NAME
        )

        fun sendClass(event: IReplyCallback, ephemeral: Boolean, cachedClass: CachedClass) {
            event.replyEmbeds(cachedClass.classEmbed)
                .addSeeAlso(cachedClass)
                .also {
                    if (!ephemeral) it.addActionRow(DeleteButtonListener.getDeleteButton(event.user))
                }
                .setEphemeral(ephemeral)
                .queue()
        }

        fun sendMethod(event: IReplyCallback, ephemeral: Boolean, cachedMethod: CachedMethod) {
            event.replyEmbeds(cachedMethod.methodEmbed)
                .addSeeAlso(cachedMethod)
                .also {
                    if (!ephemeral) it.addActionRow(DeleteButtonListener.getDeleteButton(event.user))
                }
                .setEphemeral(ephemeral)
                .queue()
        }

        fun sendField(event: IReplyCallback, ephemeral: Boolean, cachedField: CachedField) {
            event.replyEmbeds(cachedField.fieldEmbed)
                .addSeeAlso(cachedField)
                .also {
                    if (!ephemeral) it.addActionRow(DeleteButtonListener.getDeleteButton(event.user))
                }
                .setEphemeral(ephemeral)
                .queue()
        }

        fun handleClass(event: GuildSlashEvent, className: String, docIndex: DocIndex) {
            val cachedClass = docIndex.getClassDoc(className) ?: run {
                event.reply("Class '$className' does not exist").setEphemeral(true).queue()
                return
            }

            sendClass(event, false, cachedClass)
        }

        fun handleMethodDocs(event: GuildSlashEvent, className: String, identifier: String, docIndex: DocIndex) {
            if (!docIndex.classesWithMethods.contains(className)) {
                event.reply_("'$className' does not contain methods", ephemeral = true).queue()
                return
            }

            val cachedMethod = docIndex.getMethodDoc(className, identifier) ?: run {
                event.reply_("'$className' does not contain a '$identifier' method", ephemeral = true).queue()
                return
            }

            sendMethod(event, false, cachedMethod)
        }

        fun handleFieldDocs(event: GuildSlashEvent, className: String, identifier: String, docIndex: DocIndex) {
            if (!docIndex.classesWithFields.contains(className)) {
                event.reply_("'$className' does not contain fields", ephemeral = true).queue()
                return
            }

            val cachedField = docIndex.getFieldDoc(className, identifier) ?: run {
                event.reply_("'$className' does not contain a '$identifier' field", ephemeral = true).queue()
                return
            }

            sendField(event, false, cachedField)
        }

        private fun ReplyCallbackAction.addSeeAlso(cachedDoc: CachedDoc): ReplyCallbackAction {
            cachedDoc.seeAlsoReferences?.let { referenceList ->
                if (referenceList.any { it.targetType() != TargetType.UNKNOWN }) {
                    val selectionMenuBuilder = Components.selectionMenu(SEE_ALSO_SELECT_LISTENER_NAME)
                        .timeout(15, TimeUnit.MINUTES)
                        .setPlaceholder("See also")
                    for (reference in referenceList) {
                        if (reference.targetType() != TargetType.UNKNOWN) {
                            val optionValue = reference.targetType().name + ":" + reference.fullSignature()
                            if (optionValue.length > SelectMenu.ID_MAX_LENGTH) {
                                LOGGER.warn(
                                    "Option value was too large ({}) for: '{}'",
                                    optionValue.length,
                                    optionValue
                                )

                                continue
                            }

                            selectionMenuBuilder.addOption(
                                reference.text(),
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