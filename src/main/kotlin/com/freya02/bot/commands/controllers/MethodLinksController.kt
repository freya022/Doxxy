package com.freya02.bot.commands.controllers

import com.freya02.bot.commands.utils.toEditData
import com.freya02.bot.docs.DocIndexMap
import com.freya02.bot.docs.cached.CachedMethod
import com.freya02.bot.docs.index.DocIndex
import com.freya02.bot.docs.metadata.ImplementationIndex
import com.freya02.bot.docs.metadata.MethodType
import com.freya02.bot.utils.joinLengthyString
import com.freya02.botcommands.api.components.Components
import com.freya02.botcommands.api.components.event.ButtonEvent
import com.freya02.botcommands.api.components.event.StringSelectEvent
import com.freya02.botcommands.api.core.service.ServiceContainer
import com.freya02.botcommands.api.core.service.annotations.BService
import com.freya02.botcommands.api.core.service.lazy
import dev.minn.jda.ktx.interactions.components.row
import dev.minn.jda.ktx.messages.MessageCreate
import dev.minn.jda.ktx.messages.reply_
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.entities.UserSnowflake
import net.dv8tion.jda.api.exceptions.ErrorHandler
import net.dv8tion.jda.api.interactions.InteractionHook
import net.dv8tion.jda.api.interactions.components.ItemComponent
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle
import net.dv8tion.jda.api.interactions.components.selections.SelectMenu
import net.dv8tion.jda.api.requests.ErrorResponse
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.minutes

@BService
class MethodLinksController(
    serviceContainer: ServiceContainer,
    private val componentsService: Components,
    private val docIndexMap: DocIndexMap
) {
    private val logger = KotlinLogging.logger { }
    private val commonDocsController: CommonDocsController by serviceContainer.lazy()

    context(MutableList<ItemComponent>)
    fun addCachedMethodComponents(cachedMethod: CachedMethod, originalHook: InteractionHook?, caller: UserSnowflake) {
        val index = docIndexMap[cachedMethod.source]!!
        val method = runBlocking {
            index.implementationIndex.getMethod(cachedMethod.className, cachedMethod.signature)
        }

        if (method != null) {
            if (cachedMethod.implementations.isNotEmpty()) {
                val decorations = method.methodType.implementationDecorations
                componentsService.ephemeralButton(ButtonStyle.SECONDARY, decorations.label, decorations.emoji) {
                    timeout(5.minutes)
                    bindTo { sendMethodLinks(it, originalHook, caller, index, method, method.getImplementations(), decorations) }
                }.also { add(it) }
            }

            if (cachedMethod.overriddenMethods.isNotEmpty()) {
                val decorations = method.methodType.overriddenMethodsDecorations
                componentsService.ephemeralButton(ButtonStyle.SECONDARY, decorations.label, decorations.emoji) {
                    timeout(5.minutes)
                    bindTo { sendMethodLinks(it, originalHook, caller, index, method, method.getOverriddenMethods(), decorations) }
                }.also { add(it) }
            }
        } else {
            logger.trace("Found no metadata for ${cachedMethod.className}#${cachedMethod.signature}")
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
        MessageCreate {
            val (apiMethods, internalMethods) = methods.sortedBy { it.className }.partition { it.hasMethodDoc() }

            if (internalMethods.isNotEmpty()) {
                embed {
                    author(name = "${decorations.title} - ${method.className}", iconUrl = decorations.emoji.asCustom().imageUrl)
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
                    .chunked(SelectMenu.OPTIONS_MAX_AMOUNT) { methodsChunk ->
                        row(componentsService.ephemeralStringSelectMenu {
                            if (originalHook != null) {
                                timeout(originalHook.expirationTimestamp - System.currentTimeMillis(), TimeUnit.MILLISECONDS)
                            } else {
                                timeout(5.minutes)
                            }

                            bindTo { selectEvent -> onMethodLinkSelect(selectEvent, caller, index, originalHook, event) }

                            val firstChar = methodsChunk.first().className.first()
                            val lastChar = methodsChunk.last().className.first()
                            placeholder = "${decorations.placeholder} ($firstChar-$lastChar)"

                            methodsChunk.forEach {
                                val simpleQualifiedSignature = "${it.className}#${it.signature}"
                                addOption(simpleQualifiedSignature, simpleQualifiedSignature, it.methodType.emoji)
                            }
                        })
                    }
            }
        }.also { event.reply(it).setEphemeral(true).queue() }
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