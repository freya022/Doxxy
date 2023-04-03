package com.freya02.bot.commands.controllers

import com.freya02.bot.commands.slash.DeleteButtonListener.Companion.messageDeleteButton
import com.freya02.bot.commands.slash.docs.CommonDocsHandlers
import com.freya02.bot.docs.DocIndexMap
import com.freya02.bot.docs.cached.CachedClass
import com.freya02.bot.docs.cached.CachedDoc
import com.freya02.bot.docs.index.DocIndex
import com.freya02.bot.docs.index.DocSuggestion
import com.freya02.bot.docs.metadata.parser.FullSimpleClassName
import com.freya02.bot.utils.Emojis
import com.freya02.botcommands.api.components.Components
import com.freya02.botcommands.api.components.data.InteractionConstraints
import com.freya02.botcommands.api.components.event.ButtonEvent
import com.freya02.botcommands.api.core.annotations.BService
import com.freya02.botcommands.api.pagination.menu.ChoiceMenuBuilder
import com.freya02.botcommands.api.utils.ButtonContent
import com.freya02.docs.DocSourceType
import com.freya02.docs.data.TargetType
import dev.minn.jda.ktx.interactions.components.row
import dev.minn.jda.ktx.messages.Embed
import dev.minn.jda.ktx.messages.MessageCreate
import mu.KotlinLogging
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.UserSnowflake
import net.dv8tion.jda.api.interactions.components.ItemComponent
import net.dv8tion.jda.api.interactions.components.buttons.Button
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle
import net.dv8tion.jda.api.interactions.components.selections.SelectMenu
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder
import net.dv8tion.jda.api.utils.messages.MessageCreateData
import net.dv8tion.jda.api.utils.messages.MessageCreateRequest
import kotlin.time.Duration.Companion.minutes

@BService
class CommonDocsController(private val componentsService: Components, private val docIndexMap: DocIndexMap) {
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

    fun getDocMessageData(caller: Member, ephemeral: Boolean, showCaller: Boolean, cachedDoc: CachedDoc): MessageCreateData {
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
            addDocsActionRows(ephemeral, cachedDoc, caller)
        }.build()
    }

    private fun MessageCreateRequest<*>.addDocsActionRows(
        ephemeral: Boolean,
        cachedDoc: CachedDoc,
        caller: UserSnowflake
    ) {
        val list: List<ItemComponent> = buildList {
            cachedDoc.sourceLink?.let { sourceLink -> add(Button.link(sourceLink, "Source")) }

            if (cachedDoc is CachedClass) {
                val className = cachedDoc.name
                val source = cachedDoc.source
                if (cachedDoc.subclasses.isNotEmpty()) {
                    //TODO change based on current class type
                    componentsService.ephemeralButton(ButtonStyle.SECONDARY, "Subclasses", Emojis.hasOverrides) {
                        timeout(5.minutes)
                        bindTo { onSubclassesClick(it, source, className) }
                    }.also { add(it) }
                }

                if (cachedDoc.superclasses.isNotEmpty()) {
                    componentsService.ephemeralButton(ButtonStyle.SECONDARY, "Superclasses", Emojis.hasSuperclasses) {
                        timeout(5.minutes)
                        bindTo { onSuperclassesClick(it, source, className) }
                    }.also { add(it) }
                }
            }

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

    private suspend fun onSuperclassesClick(event: ButtonEvent, source: DocSourceType, className: FullSimpleClassName) {
        val index = docIndexMap[source]
        val superclasses = index!!.implementationIndex.getSuperclasses(className)

        MessageCreate {
            val cachedSuperclasses = superclasses.associateWith { index.getClassDoc(it.className) }
            val foundSuperclasses = cachedSuperclasses.filterValues { it != null }.keys
            val otherSuperclasses = cachedSuperclasses.filterValues { it == null }.keys

            if (otherSuperclasses.isNotEmpty()) {
                embed {
                    //TODO add some text perhaps
                    description = otherSuperclasses.joinToString(", ") { superclass ->
                        "[${superclass.className}](${superclass.sourceUrl})"
                    }
                }
            }

            if (foundSuperclasses.isNotEmpty()) {
                components += foundSuperclasses.take(SelectMenu.OPTIONS_MAX_AMOUNT / 5)
                    .chunked(SelectMenu.OPTIONS_MAX_AMOUNT) {
                        row(componentsService.ephemeralStringSelectMenu {
                            timeout(5.minutes)
                            //TODO bind

                            it.forEach {
                                addOption(it.className, it.className) //TODO look into adding icons if (abstract) class/interface
                            }
                        })
                    }
            }
        }.also { event.reply(it).setEphemeral(true).queue() }
    }

    private suspend fun onSubclassesClick(event: ButtonEvent, source: DocSourceType, className: FullSimpleClassName) {
        val subclasses = docIndexMap[source]!!.implementationIndex.getSubclasses(className)

        //TODO refactor with above common code
    }
}
