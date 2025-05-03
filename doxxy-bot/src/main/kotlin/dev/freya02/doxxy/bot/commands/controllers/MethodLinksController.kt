package dev.freya02.doxxy.bot.commands.controllers

import dev.freya02.doxxy.bot.docs.DocIndexMap
import dev.freya02.doxxy.bot.docs.cached.CachedMethod
import dev.freya02.doxxy.bot.docs.index.DocIndex
import dev.freya02.doxxy.bot.docs.metadata.ImplementationIndex
import dev.freya02.doxxy.bot.docs.metadata.MethodType
import dev.freya02.doxxy.bot.docs.metadata.simpleQualifiedSignature
import dev.freya02.doxxy.bot.utils.joinLengthyString
import dev.minn.jda.ktx.coroutines.await
import dev.minn.jda.ktx.interactions.components.InlineActionRow
import dev.minn.jda.ktx.interactions.components.row
import dev.minn.jda.ktx.messages.MessageCreate
import dev.minn.jda.ktx.messages.reply_
import io.github.freya022.botcommands.api.components.Buttons
import io.github.freya022.botcommands.api.components.SelectMenus
import io.github.freya022.botcommands.api.components.event.ButtonEvent
import io.github.freya022.botcommands.api.components.event.StringSelectEvent
import io.github.freya022.botcommands.api.core.BContext
import io.github.freya022.botcommands.api.core.service.ServiceContainer
import io.github.freya022.botcommands.api.core.service.annotations.BService
import io.github.freya022.botcommands.api.core.service.lazy
import io.github.freya022.botcommands.api.core.utils.toEditData
import io.github.oshai.kotlinlogging.KotlinLogging
import net.dv8tion.jda.api.components.selections.SelectMenu
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.entities.UserSnowflake
import net.dv8tion.jda.api.exceptions.ErrorHandler
import net.dv8tion.jda.api.interactions.InteractionHook
import net.dv8tion.jda.api.requests.ErrorResponse
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.minutes

@BService
class MethodLinksController(
    private val context: BContext,
    serviceContainer: ServiceContainer,
    private val buttons: Buttons,
    private val selectMenus: SelectMenus,
    private val docIndexMap: DocIndexMap
) {
    private val logger = KotlinLogging.logger { }
    private val commonDocsController: CommonDocsController by serviceContainer.lazy()

    context(InlineActionRow)
    suspend fun addCachedMethodComponents(cachedMethod: CachedMethod, originalHook: InteractionHook?, caller: UserSnowflake) {
        val index = docIndexMap[cachedMethod.source]
        val method = index.implementationIndex.getMethod(cachedMethod.className, cachedMethod.signature)

        if (method != null) {
            if (cachedMethod.implementations.isNotEmpty()) {
                val decorations = method.methodType.implementationDecorations
                +buttons.secondary(decorations.getLabel(context), decorations.emoji).ephemeral {
                    timeout(5.minutes)
                    bindTo { sendMethodLinks(it, originalHook, caller, index, method, method.getImplementations(), decorations) }
                }
            }

            if (cachedMethod.overriddenMethods.isNotEmpty()) {
                val decorations = method.methodType.overriddenMethodsDecorations
                +buttons.secondary(decorations.getLabel(context), decorations.emoji).ephemeral {
                    timeout(5.minutes)
                    bindTo { sendMethodLinks(it, originalHook, caller, index, method, method.getOverriddenMethods(), decorations) }
                }
            }
        } else {
            logger.trace { "Found no metadata for ${cachedMethod.className}#${cachedMethod.signature}" }
        }
    }

    private suspend fun sendMethodLinks(
        event: ButtonEvent,
        originalHook: InteractionHook?,
        caller: UserSnowflake,
        index: DocIndex,
        method: ImplementationIndex.Method,
        methods: List<ImplementationIndex.Method>,
        decorations: MethodType.Decorations
    ) {
        val message = MessageCreate {
            val (apiMethods, internalMethods) = methods.sortedBy { it.className }.partition { it.hasMethodDoc() }

            if (internalMethods.isNotEmpty()) {
                embed {
                    author(name = "${decorations.getTitle(context)} - ${method.className}", iconUrl = decorations.emoji.imageUrl)
                    description = internalMethods
                        .joinLengthyString(
                            separator = ", ",
                            truncated = " and more...",
                            lengthLimit = MessageEmbed.DESCRIPTION_MAX_LENGTH
                        ) { internalMethod -> "[${internalMethod.className}#${internalMethod.signature}](${internalMethod.sourceLink})" }
                }
            }

            if (apiMethods.isNotEmpty()) {
                components += apiMethods.take(SelectMenu.OPTIONS_MAX_AMOUNT * 5)
                    .chunked(SelectMenu.OPTIONS_MAX_AMOUNT)
                    .map { methodsChunk ->
                        row(selectMenus.stringSelectMenu().ephemeral {
                            if (originalHook != null) {
                                timeout(originalHook.expirationTimestamp - System.currentTimeMillis(), TimeUnit.MILLISECONDS)
                            } else {
                                timeout(5.minutes)
                            }

                            bindTo { selectEvent -> onMethodLinkSelect(selectEvent, caller, index, originalHook, event) }

                            val firstChar = methodsChunk.first().className.first()
                            val lastChar = methodsChunk.last().className.first()
                            placeholder = "${decorations.getPlaceholder(context)} ($firstChar-$lastChar)"

                            val addedValues = hashSetOf<String>()
                            methodsChunk.forEach {
                                val simpleQualifiedSignature = it.simpleQualifiedSignature
                                if (addedValues.add(simpleQualifiedSignature)) {
                                    addOption(simpleQualifiedSignature, simpleQualifiedSignature, it.methodType.emoji)
                                } else {
                                    logger.warn { "Already added '$simpleQualifiedSignature' from ${method.simpleQualifiedSignature}" }
                                }
                            }
                        })
                    }
            }
        }
        event.reply(message).setEphemeral(true).await()
    }

    private suspend fun onMethodLinkSelect(
        selectEvent: StringSelectEvent,
        caller: UserSnowflake,
        index: DocIndex,
        originalHook: InteractionHook?, //probably from /docs or similar
        buttonEvent: ButtonEvent
    ) {
        val selectedQualifiedMethodSignature = selectEvent.values.single()
        val isSameCaller = selectEvent.user.idLong == caller.idLong
        val cachedMethod = index.getMethodDoc(selectedQualifiedMethodSignature)
            ?: return selectEvent.reply_("This method no longer exists", ephemeral = true).queue()

        val createData = commonDocsController.getDocMessageData(
            originalHook,
            selectEvent.member!!,
            ephemeral = !isSameCaller,
            showCaller = false,
            cachedMethod
        )

        if (isSameCaller) {
            selectEvent.deferEdit().queue()
            val editAction = when {
                originalHook != null -> originalHook.editOriginal(createData.toEditData())
                else -> buttonEvent.message.editMessage(createData.toEditData())
            }
            //I guess this can happen if the user clicks the button and then immediately deletes the message
            editAction.queue(null, ErrorHandler().ignore(ErrorResponse.UNKNOWN_MESSAGE))
        } else {
            selectEvent.reply(createData).setEphemeral(true).queue()
        }
    }
}