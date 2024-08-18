package com.freya02.bot.commands.controllers

import com.freya02.bot.docs.DocIndexMap
import com.freya02.bot.docs.cached.CachedClass
import com.freya02.bot.docs.index.DocIndex
import com.freya02.bot.docs.metadata.ClassType
import com.freya02.bot.docs.metadata.ImplementationIndex
import com.freya02.bot.utils.joinLengthyString
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
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.entities.UserSnowflake
import net.dv8tion.jda.api.exceptions.ErrorHandler
import net.dv8tion.jda.api.interactions.InteractionHook
import net.dv8tion.jda.api.interactions.components.ItemComponent
import net.dv8tion.jda.api.interactions.components.selections.SelectMenu
import net.dv8tion.jda.api.requests.ErrorResponse
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.minutes

@BService
class ClassLinksController(
    private val context: BContext,
    serviceContainer: ServiceContainer,
    private val buttons: Buttons,
    private val selectMenus: SelectMenus,
    private val docIndexMap: DocIndexMap
) {
    private val logger = KotlinLogging.logger { }
    private val commonDocsController: CommonDocsController by serviceContainer.lazy()

    context(MutableList<ItemComponent>)
    suspend fun addCachedClassComponents(cachedDoc: CachedClass, originalHook: InteractionHook?, caller: UserSnowflake) {
        val index = docIndexMap[cachedDoc.source]
        val clazz = index.implementationIndex.getClass(cachedDoc.name)

        if (clazz != null) {
            if (cachedDoc.subclasses.isNotEmpty()) {
                val decorations = clazz.classType.subDecorations
                buttons.secondary(decorations.getLabel(context), decorations.emoji).ephemeral {
                    timeout(5.minutes)
                    bindTo { sendClassLinks(it, originalHook, caller, index, clazz, clazz.getSubclasses(), decorations) }
                }.also { add(it) }
            }

            if (cachedDoc.superclasses.isNotEmpty()) {
                val decorations = clazz.classType.superDecorations
                buttons.secondary(decorations.getLabel(context), decorations.emoji).ephemeral {
                    timeout(5.minutes)
                    bindTo { sendClassLinks(it, originalHook, caller, index, clazz, clazz.getSuperclasses(), decorations) }
                }.also { add(it) }
            }
        } else {
            logger.trace { "Found no metadata for ${cachedDoc.name}" }
        }
    }

    private suspend fun sendClassLinks(
        event: ButtonEvent,
        originalHook: InteractionHook?,
        caller: UserSnowflake,
        index: DocIndex,
        clazz: ImplementationIndex.Class,
        classes: List<ImplementationIndex.Class>,
        decorations: ClassType.Decorations
    ) {
        val message = MessageCreate {
            val (apiClasses, internalClasses) = classes.sortedBy { it.className }.partition { it.hasClassDoc() }

            if (internalClasses.isNotEmpty()) {
                embed {
                    author(name = "${decorations.getTitle(context)} - ${clazz.className}", iconUrl = decorations.emoji.asCustom().imageUrl)
                    description = internalClasses
                        .joinLengthyString(
                            separator = ", ",
                            truncated = " and more...",
                            lengthLimit = MessageEmbed.DESCRIPTION_MAX_LENGTH
                        ) { internalClass -> "[${internalClass.className}](${internalClass.sourceLink})" }
                }
            }

            if (apiClasses.isNotEmpty()) {
                components += apiClasses.take(SelectMenu.OPTIONS_MAX_AMOUNT * 5)
                    .chunked(SelectMenu.OPTIONS_MAX_AMOUNT)
                    .map { superclassesChunk ->
                        row(selectMenus.stringSelectMenu().ephemeral {
                            if (originalHook != null) {
                                timeout(originalHook.expirationTimestamp - System.currentTimeMillis(), TimeUnit.MILLISECONDS)
                            } else {
                                timeout(5.minutes)
                            }

                            bindTo { selectEvent -> onClassLinkSelect(
                                selectEvent,
                                caller,
                                index,
                                originalHook,
                                event
                            ) }

                            val firstChar = superclassesChunk.first().className.first()
                            val lastChar = superclassesChunk.last().className.first()
                            placeholder = "${decorations.getPlaceholder(context)} ($firstChar-$lastChar)"

                            superclassesChunk.forEach {
                                addOption(it.className, it.className, it.classType.emoji)
                            }
                        })
                    }
            }
        }
        event.reply(message).setEphemeral(true).queue()
    }

    private suspend fun onClassLinkSelect(
        selectEvent: StringSelectEvent,
        caller: UserSnowflake,
        index: DocIndex,
        originalHook: InteractionHook?, //probably from /docs or similar
        buttonEvent: ButtonEvent
    ) {
        val selectedClassName = selectEvent.values.single()
        val isSameCaller = selectEvent.user.idLong == caller.idLong
        val cachedDoc = index.getClassDoc(selectedClassName)
            ?: return selectEvent.reply_("This class no longer exists", ephemeral = true).queue()

        val createData = commonDocsController.getDocMessageData(
            originalHook,
            selectEvent.member!!,
            ephemeral = !isSameCaller,
            showCaller = false,
            cachedDoc
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