package com.freya02.bot.commands.controllers

import com.freya02.bot.commands.slash.DeleteButtonListener.Companion.messageDeleteButton
import com.freya02.bot.commands.slash.docs.CommonDocsHandlers
import com.freya02.bot.docs.cached.CachedClass
import com.freya02.bot.docs.cached.CachedDoc
import com.freya02.bot.docs.cached.CachedMethod
import com.freya02.bot.docs.index.DocIndex
import com.freya02.bot.docs.index.DocSuggestion
import com.freya02.bot.utils.Emojis
import com.freya02.botcommands.api.components.Components
import com.freya02.botcommands.api.components.data.InteractionConstraints
import com.freya02.botcommands.api.core.annotations.BService
import com.freya02.botcommands.api.pagination.menu.ChoiceMenuBuilder
import com.freya02.botcommands.api.utils.ButtonContent
import com.freya02.docs.DocSourceType
import com.freya02.docs.data.TargetType
import dev.minn.jda.ktx.messages.Embed
import mu.KotlinLogging
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.UserSnowflake
import net.dv8tion.jda.api.interactions.InteractionHook
import net.dv8tion.jda.api.interactions.components.ItemComponent
import net.dv8tion.jda.api.interactions.components.buttons.Button
import net.dv8tion.jda.api.interactions.components.selections.SelectMenu
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder
import net.dv8tion.jda.api.utils.messages.MessageCreateData
import net.dv8tion.jda.api.utils.messages.MessageCreateRequest
import kotlin.time.Duration.Companion.minutes

@BService
class CommonDocsController(
    private val componentsService: Components,
    private val classLinksController: ClassLinksController,
    private val methodLinksController: MethodLinksController
) {
    private val logger = KotlinLogging.logger { }

    fun buildDocSuggestionsMenu(docIndex: DocIndex, suggestions: List<DocSuggestion>, user: UserSnowflake, block: ChoiceMenuBuilder<DocSuggestion>.() -> Unit) =
        ChoiceMenuBuilder(componentsService, suggestions)
            .setConstraints(InteractionConstraints.ofUsers(user))
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

    fun getDocMessageData(originalHook: InteractionHook?, caller: Member, ephemeral: Boolean, showCaller: Boolean, cachedDoc: CachedDoc): MessageCreateData {
        return MessageCreateBuilder().apply {
            addEmbeds(cachedDoc.embed.let {
                when {
                    showCaller -> Embed {
                        builder.copyFrom(it)
                        author(caller.effectiveName, iconUrl = caller.effectiveAvatarUrl)
                    }
                    else -> it
                }
            })
            addDocsSeeAlso(cachedDoc)
            addDocsActionRows(originalHook, ephemeral, cachedDoc, caller)
        }.build()
    }

    private fun MessageCreateRequest<*>.addDocsActionRows(
        originalHook: InteractionHook?,
        ephemeral: Boolean,
        cachedDoc: CachedDoc,
        caller: UserSnowflake
    ) {
        val list: List<ItemComponent> = buildList {
            cachedDoc.sourceLink?.let { sourceLink -> add(Button.link(sourceLink, "Source")) }

            if (cachedDoc is CachedClass)
                classLinksController.addCachedClassComponents(cachedDoc, originalHook)
            else if (cachedDoc is CachedMethod)
                methodLinksController.addCachedMethodComponents(cachedDoc, originalHook)

            if (!ephemeral) add(componentsService.messageDeleteButton(caller))
        }

        if (list.isNotEmpty()) {
            addActionRow(list)
        }
    }

    private fun MessageCreateRequest<*>.addDocsSeeAlso(cachedDoc: CachedDoc) {
        cachedDoc.seeAlsoReferences.let { referenceList ->
            if (referenceList.any { it.targetType != TargetType.UNKNOWN }) {
                val selectMenu = componentsService.persistentStringSelectMenu {
                    bindTo(CommonDocsHandlers.SEE_ALSO_SELECT_LISTENER_NAME, cachedDoc.source.id)
                    timeout(15.minutes)
                    placeholder = "See also"

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

                            addOption(reference.text, optionValue, Emojis.clipboard)
                        }
                    }
                }

                addActionRow(selectMenu)
            }
        }
    }
}
