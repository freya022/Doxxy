package com.freya02.bot.commands.controllers

import com.freya02.bot.docs.DocIndexMap
import com.freya02.bot.docs.cached.CachedMethod
import com.freya02.bot.docs.index.DocIndex
import com.freya02.bot.docs.metadata.ImplementationIndex
import com.freya02.bot.docs.metadata.MethodType
import com.freya02.bot.utils.joinLengthyString
import com.freya02.botcommands.api.components.Components
import com.freya02.botcommands.api.components.event.ButtonEvent
import com.freya02.botcommands.api.core.ServiceContainer
import com.freya02.botcommands.api.core.annotations.BService
import dev.minn.jda.ktx.interactions.components.row
import dev.minn.jda.ktx.messages.MessageCreate
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.interactions.InteractionHook
import net.dv8tion.jda.api.interactions.components.ItemComponent
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle
import net.dv8tion.jda.api.interactions.components.selections.SelectMenu
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
    fun addCachedMethodComponents(cachedMethod: CachedMethod, originalHook: InteractionHook?) {
        val index = docIndexMap[cachedMethod.source]!!
        val method = runBlocking {
            index.implementationIndex.getMethod(cachedMethod.className, cachedMethod.signature)
        }

        if (method != null) {
            if (cachedMethod.implementations.isNotEmpty()) {
                val decorations = method.methodType.implementationDecorations
                componentsService.ephemeralButton(ButtonStyle.SECONDARY, decorations.label, decorations.emoji) {
                    timeout(5.minutes)
                    bindTo { sendMethodLinks(it, originalHook, index, method, method.getImplementations(), decorations) }
                }.also { add(it) }
            }
        } else {
            logger.trace("Found no metadata for ${cachedMethod.className}#${cachedMethod.signature}")
        }
    }

    private suspend fun sendMethodLinks(
        event: ButtonEvent,
        originalHook: InteractionHook?,
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

                            val slashUserId = event.message.interaction!!.user.idLong
                            bindTo { selectEvent -> TODO() }

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
}